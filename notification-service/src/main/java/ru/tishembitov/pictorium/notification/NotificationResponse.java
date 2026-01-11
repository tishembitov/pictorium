package ru.tishembitov.pictorium.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String recipientId,
        String actorId,
        NotificationType type,
        NotificationStatus status,
        UUID referenceId,
        UUID secondaryRefId,
        String previewText,
        String previewImageId,
        Instant createdAt,
        Instant readAt
) {}