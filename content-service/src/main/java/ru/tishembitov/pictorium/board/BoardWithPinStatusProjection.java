package ru.tishembitov.pictorium.board;

import java.time.Instant;
import java.util.UUID;

public interface BoardWithPinStatusProjection {
    UUID getId();
    String getUserId();
    String getTitle();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    boolean getHasPin();
    int getPinCount();
}