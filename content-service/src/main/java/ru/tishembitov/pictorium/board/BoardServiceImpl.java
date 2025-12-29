package ru.tishembitov.pictorium.board;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.BadRequestException;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.like.LikeRepository;
import ru.tishembitov.pictorium.pin.*;
import ru.tishembitov.pictorium.savedPin.SavedPinRepository;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final PinRepository pinRepository;
    private final SavedPinRepository savedPinRepository;
    private final LikeRepository likeRepository;
    private final BoardMapper boardMapper;
    private final PinMapper pinMapper;
    private final PinService pinService;

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
    public List<BoardResponse> getUserBoards(String userId) {
        List<Board> boards = boardRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return boardMapper.toResponseList(boards);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardResponse> getMyBoards() {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        return getUserBoards(currentUserId);
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
    public PinResponse savePinToBoard(UUID boardId, UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Board board = boardRepository.findByIdAndUserId(boardId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found or access denied"));

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        if (boardRepository.existsPinInBoard(boardId, pinId)) {
            return pinMapper.toResponse(pin, pinService.getPinInteractionDto(pinId));
        }

        boolean wasAlreadySaved = boardRepository.isPinSavedByUser(currentUserId, pinId);

        board.getPins().add(pin);
        boardRepository.save(board);

        if (!wasAlreadySaved) {
            pinRepository.incrementSaveCount(pinId);
        }

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

        boolean wasAlreadySaved = boardRepository.isPinSavedByUser(currentUserId, pinId);

        int savedCount = 0;
        for (UUID boardId : boardIds) {
            Board board = boardRepository.findByIdAndUserId(boardId, currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Board not found or access denied: " + boardId));

            if (!boardRepository.existsPinInBoard(boardId, pinId)) {
                board.getPins().add(pin);
                boardRepository.save(board);
                savedCount++;
            }
        }

        if (!wasAlreadySaved && savedCount > 0) {
            pinRepository.incrementSaveCount(pinId);
        }

        log.info("Pin saved to {} boards: pinId={}, userId={}", savedCount, pinId, currentUserId);

        return pinMapper.toResponse(pin, pinService.getPinInteractionDto(pinId));
    }

    @Override
    public void removePinFromBoard(UUID boardId, UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Board board = boardRepository.findByIdWithPinsAndUserId(boardId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found or access denied"));

        Pin pin = board.getPins().stream()
                .filter(p -> p.getId().equals(pinId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found in board"));

        board.getPins().remove(pin);
        boardRepository.save(board);

        boolean stillSavedInBoards = boardRepository.isPinSavedByUser(currentUserId, pinId);
        boolean savedToProfile = savedPinRepository.existsByUserIdAndPinId(currentUserId, pinId);

        if (!stillSavedInBoards && !savedToProfile) {
            pinRepository.decrementSaveCount(pinId);
        }

        log.info("Pin removed from board: boardId={}, pinId={}, userId={}", boardId, pinId, currentUserId);
    }

    @Override
    public void removePinFromAllBoards(UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        int removedCount = boardRepository.removePinFromUserBoards(currentUserId, pinId);

        if (removedCount == 0) {
            throw new ResourceNotFoundException("Pin is not saved in any of your boards");
        }

        boolean savedToProfile = savedPinRepository.existsByUserIdAndPinId(currentUserId, pinId);

        if (!savedToProfile) {
            pinRepository.decrementSaveCount(pinId);
        }

        log.info("Pin removed from all boards: pinId={}, userId={}, boardCount={}",
                pinId, currentUserId, removedCount);
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

        Set<UUID> pinIds = board.getPins().stream()
                .map(Pin::getId)
                .collect(Collectors.toSet());

        if (!pinIds.isEmpty()) {
            Set<UUID> pinsToDecrement = boardRepository.findPinsNotSavedElsewhere(
                    currentUserId, pinIds, boardId);

            if (!pinsToDecrement.isEmpty()) {
                pinRepository.decrementSaveCountBatch(pinsToDecrement);
            }
        }

        boardRepository.delete(board);

        log.info("Board deleted: id={}, userId={}", boardId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPinSavedByCurrentUser(UUID pinId) {
        return SecurityUtils.getCurrentUserId()
                .map(userId -> boardRepository.isPinSavedByUser(userId, pinId))
                .orElse(false);
    }

    private void checkBoardOwnership(Board board, String userId) {
        if (!board.getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to modify this board");
        }
    }
}