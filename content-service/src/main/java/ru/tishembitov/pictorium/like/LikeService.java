package ru.tishembitov.pictorium.like;

import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

public interface LikeService {
    PinResponse likePin(UUID pinId);
    void unlikePin(UUID pinId);
}
