package ru.tishembitov.pictorium.chat;

import ru.tishembitov.pictorium.message.MessageType;

import java.time.Instant;
import java.util.UUID;

public record ChatResponse(
        UUID id,
        String recipientId,
        String lastMessage,
        Instant lastMessageTime,
        int unreadCount,
        Instant createdAt,
        Instant updatedAt,

        MessageType lastMessageType,
        String lastMessageImageId
) {}