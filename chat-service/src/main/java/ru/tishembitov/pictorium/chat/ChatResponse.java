package ru.tishembitov.pictorium.chat;

import java.time.Instant;
import java.util.UUID;

public record ChatResponse(
        UUID id,
        String recipientId,
        String lastMessage,
        Instant lastMessageTime,
        int unreadCount,
        Instant createdAt,
        Instant updatedAt
) {}