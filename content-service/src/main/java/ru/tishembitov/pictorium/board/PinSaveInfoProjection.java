package ru.tishembitov.pictorium.board;

import java.util.UUID;

public interface PinSaveInfoProjection {
    UUID getPinId();
    String getLastBoardName(); 
    Long getBoardCount();
}