package ru.tishembitov.pictorium.like;

import java.time.Instant;
import java.util.UUID;

public record LikeResponse (
        UUID id,
        String userId,
        UUID pinId,
        UUID commentId,
        Instant createdAt
){}
