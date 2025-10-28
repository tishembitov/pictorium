package ru.tishembitov.pictorium.selectedBoard;


import ru.tishembitov.pictorium.board.BoardResponse;

import java.util.UUID;

public interface SelectedBoardService {

    void selectBoard(UUID boardId);

    BoardResponse getSelectedBoard();

    void disableBoard();
}
