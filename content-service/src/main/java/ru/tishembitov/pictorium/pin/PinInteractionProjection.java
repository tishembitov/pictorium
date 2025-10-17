package ru.tishembitov.pictorium.pin;

import java.util.UUID;

public record PinInteractionProjection(
        UUID pinId,
        Boolean isLiked,
        Boolean isSaved
) {}
