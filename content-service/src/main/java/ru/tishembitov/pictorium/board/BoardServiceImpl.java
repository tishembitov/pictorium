package ru.tishembitov.pictorium.board;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.BadRequestException;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.pin.Pin;
import ru.tishembitov.pictorium.pin.PinInteractionDto;
import ru.tishembitov.pictorium.pin.PinMapper;
import ru.tishembitov.pictorium.pin.PinRepository;
import ru.tishembitov.pictorium.pin.PinResponse;
import ru.tishembitov.pictorium.pin.PinService;
import ru.tishembitov.pictorium.selectedBoard.SelectedBoard;
import ru.tishembitov.pictorium.selectedBoard.SelectedBoardRepository;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final BoardPinRepository boardPinRepository;
    private final PinRepository pinRepository;
    private final SelectedBoardRepository selectedBoardRepository;
    private final BoardMapper boardMapper;
    private final PinMapper pinMapper;
    private final PinService pinService;
    private final EntityManager entityManager;

    @Override
    public BoardResponse createBoard(BoardCreateRequest request) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        if (boardRepository.existsByUserIdAndTitle(currentUserId, request.title().trim())) {
            throw new BadRequestException("Board with title '" + request.title() + "' already exists");
        }

        Board board = boardMapper.toEntity(request, currentUserId);
        Board savedBoard = boardRepository.save(board);

        log.info("Board created: id={}, userId={}", savedBoard.getId(), currentUserId);
        return boardMapper.toResponse(savedBoard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardResponse> getMyBoards() {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        return getUserBoards(currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardResponse> getUserBoards(String userId) {
        List<Board> boards = boardRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return boardMapper.toResponseList(boards);
    }

    @Override
    @Transactional(readOnly = true)
    public BoardResponse getBoardById(UUID boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId));
        return boardMapper.toResponse(board);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardWithPinStatusResponse> getMyBoardsForPin(UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        if (!pinRepository.existsById(pinId)) {
            throw new ResourceNotFoundException("Pin not found with id: " + pinId);
        }

        List<BoardWithPinStatusProjection> projections =
                boardRepository.findUserBoardsWithPinStatus(currentUserId, pinId);

        return boardMapper.toWithPinStatusResponseList(projections);
    }

    @Override
    public BoardResponse updateBoard(UUID boardId, BoardUpdateRequest request) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId));

        checkBoardOwnership(board, currentUserId);

        String newTitle = request.title().trim();

        if (!board.getTitle().equals(newTitle)) {
            if (boardRepository.existsByUserIdAndTitle(currentUserId, newTitle)) {
                throw new BadRequestException("Board with title '" + newTitle + "' already exists");
            }
        }

        boardMapper.updateFromRequest(request, board);
        Board updatedBoard = boardRepository.save(board);

        log.info("Board updated: id={}, userId={}", boardId, currentUserId);
        return boardMapper.toResponse(updatedBoard);
    }


    @Override
    public PinResponse savePinToBoard(UUID boardId, UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Board board = boardRepository.findByIdAndUserId(boardId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found or access denied"));

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        if (boardPinRepository.existsByBoardIdAndPinId(boardId, pinId)) {
            return pinMapper.toResponse(pin, pinService.getPinInteractionDto(pinId));
        }

        boolean wasAlreadySaved = boardPinRepository.isPinSavedByUser(currentUserId, pinId);

        BoardPin boardPin = BoardPin.builder()
                .board(board)
                .pin(pin)
                .build();
        boardPinRepository.save(boardPin);

        if (!wasAlreadySaved) {
            pinRepository.incrementSaveCount(pinId);
        }

        entityManager.flush();
        entityManager.refresh(pin);

        updateSelectedBoard(currentUserId, board);

        log.info("Pin saved to board: boardId={}, pinId={}, userId={}", boardId, pinId, currentUserId);

        return pinMapper.toResponse(pin, pinService.getPinInteractionDto(pinId));
    }

    @Override
    public PinResponse savePinToBoards(UUID pinId, List<UUID> boardIds) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        if (boardIds == null || boardIds.isEmpty()) {
            throw new BadRequestException("At least one board must be selected");
        }

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        boolean wasAlreadySaved = boardPinRepository.isPinSavedByUser(currentUserId, pinId);

        Board lastBoard = null;
        int savedCount = 0;

        for (UUID boardId : boardIds) {
            Board board = boardRepository.findByIdAndUserId(boardId, currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Board not found or access denied: " + boardId));

            if (!boardPinRepository.existsByBoardIdAndPinId(boardId, pinId)) {
                BoardPin boardPin = BoardPin.builder()
                        .board(board)
                        .pin(pin)
                        .build();
                boardPinRepository.save(boardPin);
                lastBoard = board;
                savedCount++;
            }
        }

        if (!wasAlreadySaved && savedCount > 0) {
            pinRepository.incrementSaveCount(pinId);
        }

        entityManager.flush();
        entityManager.refresh(pin);

        if (lastBoard != null) {
            updateSelectedBoard(currentUserId, lastBoard);
        }

        log.info("Pin saved to {} boards: pinId={}, userId={}", savedCount, pinId, currentUserId);

        return pinMapper.toResponse(pin, pinService.getPinInteractionDto(pinId));
    }

    @Override
    public void removePinFromBoard(UUID boardId, UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        if (boardRepository.findByIdAndUserId(boardId, currentUserId).isEmpty()) {
            throw new ResourceNotFoundException("Board not found or access denied");
        }

        BoardPin boardPin = boardPinRepository.findByBoardIdAndPinId(boardId, pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found in board"));

        boardPinRepository.delete(boardPin);

        boolean stillSaved = boardPinRepository.isPinSavedByUser(currentUserId, pinId);
        if (!stillSaved) {
            pinRepository.decrementSaveCount(pinId);
        }

        log.info("Pin removed from board: boardId={}, pinId={}, userId={}",
                boardId, pinId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PinResponse> getBoardPins(UUID boardId, Pageable pageable) {
        if (!boardRepository.existsById(boardId)) {
            throw new ResourceNotFoundException("Board not found with id: " + boardId);
        }

        Page<Pin> pins = pinRepository.findByBoardId(boardId, pageable);

        if (pins.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<UUID> pinIds = pins.getContent().stream()
                .map(Pin::getId)
                .collect(Collectors.toSet());

        Map<UUID, PinInteractionDto> interactions =
                pinService.getPinInteractionDtosBatch(pinIds);

        return pins.map(pin -> {
            PinInteractionDto interaction = interactions.getOrDefault(
                    pin.getId(),
                    PinInteractionDto.empty()
            );
            return pinMapper.toResponse(pin, interaction);
        });
    }

    @Override
    public void deleteBoard(UUID boardId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Board board = boardRepository.findByIdWithPins(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId));

        checkBoardOwnership(board, currentUserId);

        Set<UUID> pinIds = board.getBoardPins().stream()
                .map(bp -> bp.getPin().getId())
                .collect(Collectors.toSet());

        if (!pinIds.isEmpty()) {
            Set<UUID> pinsToDecrement = boardRepository.findPinsOnlyInBoard(
                    currentUserId, pinIds, boardId);

            if (!pinsToDecrement.isEmpty()) {
                pinRepository.decrementSaveCountBatch(pinsToDecrement);
            }
        }

        selectedBoardRepository.findByUserId(currentUserId)
                .filter(sb -> sb.getSelectedBoard() != null &&
                        sb.getSelectedBoard().getId().equals(boardId))
                .ifPresent(selectedBoardRepository::delete);

        boardRepository.delete(board);

        log.info("Board deleted: id={}, userId={}", boardId, currentUserId);
    }

    @Override
    public BoardResponse createBoardAndSavePin(BoardCreateRequest request, UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        if (boardRepository.existsByUserIdAndTitle(currentUserId, request.title().trim())) {
            throw new BadRequestException("Board with title '" + request.title() + "' already exists");
        }

        boolean wasAlreadySaved = boardPinRepository.isPinSavedByUser(currentUserId, pinId);

        Board board = boardMapper.toEntity(request, currentUserId);
        Board savedBoard = boardRepository.save(board);

        BoardPin boardPin = BoardPin.builder()
                .board(savedBoard)
                .pin(pin)
                .build();
        boardPinRepository.save(boardPin);

        if (!wasAlreadySaved) {
            pinRepository.incrementSaveCount(pinId);
        }

        updateSelectedBoard(currentUserId, savedBoard);

        log.info("Board created with pin: boardId={}, pinId={}, userId={}",
                savedBoard.getId(), pinId, currentUserId);

        return boardMapper.toResponse(savedBoard);
    }

    private void checkBoardOwnership(Board board, String userId) {
        if (!board.getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to modify this board");
        }
    }

    private void updateSelectedBoard(String userId, Board board) {
        SelectedBoard selectedBoard = selectedBoardRepository.findByUserId(userId)
                .orElseGet(() -> SelectedBoard.builder()
                        .userId(userId)
                        .build());

        selectedBoard.setSelectedBoard(board);
        selectedBoardRepository.save(selectedBoard);
    }
}