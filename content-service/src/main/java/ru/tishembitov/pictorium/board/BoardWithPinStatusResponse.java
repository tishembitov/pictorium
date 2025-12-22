package ru.tishembitov.pictorium.board;

import java.time.Instant;
import java.util.UUID;

public record BoardWithPinStatusResponse(
        UUID id,
        String userId,
        String title,
        Instant createdAt,
        Instant updatedAt,
        boolean hasPin,      // пин уже сохранён в эту доску
        int pinCount         // общее количество пинов в доске
) {}