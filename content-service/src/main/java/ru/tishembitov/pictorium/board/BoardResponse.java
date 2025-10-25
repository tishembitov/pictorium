package ru.tishembitov.pictorium.board;

import java.time.Instant;
import java.util.UUID;

public record BoardResponse(
     UUID id,
     String userId,
     String title,
     Instant createdAt,
     Instant updatedAt,
     Integer pinCount
) {}
