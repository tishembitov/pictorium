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
import ru.tishembitov.pictorium.pin.*;
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
    private final BoardMapper boardMapper;
    private final PinMapper pinMapper;
    private final PinService pinService;


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

    @Transactional(readOnly = true)
    public List<BoardResponse> getUserBoards(String userId) {
        List<Board> boards = boardRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return boardMapper.toResponseList(boards);
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> getMyBoards() {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        return getUserBoards(currentUserId);
    }

    @Transactional(readOnly = true)
    public BoardResponse getBoardById(UUID boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId));
        return boardMapper.toResponse(board);
    }

    public void addPinToBoard(UUID boardId, UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId));

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        if (boardRepository.existsPinOnBoard(boardId, pinId)) {
            throw new BadRequestException("Pin is already in the board");
        }

        board.getPins().add(pin);
        boardRepository.save(board);

        // TODO: Если нужно, отправить уведомление автору пина
        // if (!pin.getAuthorId().equals(userId)) {
        //     notificationService.sendPinSavedNotification(pin.getAuthorId(), userId, pinId);
        // }

        log.info("Pin added to board: boardId={}, pinId={}, userId={}", boardId, pinId, currentUserId);
    }

    public void removePinFromBoard(UUID boardId, UUID pinId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Board board = boardRepository.findByIdAndUserId(boardId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found or access denied"));

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        if (!board.getPins().remove(pin)) {
            throw new ResourceNotFoundException("Pin not found in board");
        }

        boardRepository.save(board);
        log.info("Pin removed from board: boardId={}, pinId={}, userId={}", boardId, pinId, currentUserId);
    }

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
            return pinMapper.toResponse(pin, interaction.isLiked(), interaction.isSaved());
        });
    }

    public void deleteBoard(UUID boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId));

        String currentUserId = SecurityUtils.requireCurrentUserId();
        checkBoardOwnership(board, currentUserId);
        boardRepository.delete(board);
        log.info("Board deleted: id={}, userId={}", boardId, currentUserId);
    }

    private void checkBoardOwnership(Board board, String userId) {
        if (!board.getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to modify this pin");
        }
    }

}