package ru.tishembitov.pictorium.message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID chatId,
        String senderId,
        String receiverId,
        String content,
        MessageType type,
        MessageState state,
        String imageId,
        Instant createdAt
) {}