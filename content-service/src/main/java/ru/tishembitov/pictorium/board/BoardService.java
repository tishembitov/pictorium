package ru.tishembitov.pictorium.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.List;
import java.util.UUID;

public interface BoardService {

    BoardResponse createBoard(BoardCreateRequest request);

    List<BoardResponse> getMyBoards();

    List<BoardResponse> getUserBoards(String userId);

    BoardResponse getBoardById(UUID boardId);

    List<BoardWithPinStatusResponse> getMyBoardsForPin(UUID pinId);

    PinResponse savePinToBoard(UUID boardId, UUID pinId);

    PinResponse savePinToBoards(UUID pinId, List<UUID> boardIds);

    void removePinFromBoard(UUID boardId, UUID pinId);

    Page<PinResponse> getBoardPins(UUID boardId, Pageable pageable);

    void deleteBoard(UUID boardId);

    BoardResponse createBoardAndSavePin(BoardCreateRequest request, UUID pinId);
}