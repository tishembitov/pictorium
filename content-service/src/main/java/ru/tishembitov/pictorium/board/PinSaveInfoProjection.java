package ru.tishembitov.pictorium.board;

import java.util.UUID;

public interface PinSaveInfoProjection {
    UUID getPinId();
    String getFirstBoardName();
    Long getBoardCount();
}