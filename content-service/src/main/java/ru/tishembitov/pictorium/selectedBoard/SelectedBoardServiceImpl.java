package ru.tishembitov.pictorium.selectedBoard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.board.Board;
import ru.tishembitov.pictorium.board.BoardMapper;
import ru.tishembitov.pictorium.board.BoardRepository;
import ru.tishembitov.pictorium.board.BoardResponse;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SelectedBoardServiceImpl implements SelectedBoardService {

    private final BoardRepository boardRepository;
    private final SelectedBoardRepository selectedBoardRepository;
    private final BoardMapper boardMapper;

    @Override
    public void selectBoard(UUID boardId) {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId));

        checkBoardOwnership(board, currentUserId);

        SelectedBoard selectedBoard = selectedBoardRepository.findByUserId(currentUserId)
                .orElseGet(() -> SelectedBoard.builder()
                        .userId(currentUserId)
                        .build());

        selectedBoard.setSelectedBoard(board);
        selectedBoardRepository.save(selectedBoard);
        log.info("Board {} selected for user {}", boardId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public BoardResponse getSelectedBoard() {
        String currentUserId = SecurityUtils.requireCurrentUserId();

        return selectedBoardRepository.findByUserIdWithBoard(currentUserId)
                .map(SelectedBoard::getSelectedBoard)
                .map(boardMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No selected board found for user"));
    }

    @Override
    public void disableBoard() {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        selectedBoardRepository.findByUserId(currentUserId)
                .ifPresent(sb -> {
                    selectedBoardRepository.delete(sb);
                    log.info("Selected board disabled for user {}", currentUserId);
                });
    }

    private void checkBoardOwnership(Board board, String userId) {
        if (!board.getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to modify this board");
        }
    }
}
