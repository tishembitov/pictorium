package ru.tishembitov.pictorium.board;

import ru.tishembitov.pictorium.kafka.event.BoardEvent;

import java.util.Optional;

public interface BoardIndexService {

    void indexBoard(BoardEvent event);

    void updateBoard(BoardEvent event);

    void updateBoardPinCount(BoardEvent event);

    void deleteBoard(String boardId);

    Optional<BoardDocument> findById(String boardId);

    long count();
}