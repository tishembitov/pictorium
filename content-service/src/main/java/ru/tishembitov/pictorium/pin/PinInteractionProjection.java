package ru.tishembitov.pictorium.pin;

import java.util.UUID;

public interface PinInteractionProjection{
        UUID getId();
        Boolean isLiked();
        Boolean isSaved();
}
