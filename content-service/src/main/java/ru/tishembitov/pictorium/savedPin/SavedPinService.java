package ru.tishembitov.pictorium.savedPin;

import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

public interface SavedPinService {

    PinResponse savePin(UUID pinId);

    void unsavePin(UUID pinId);

}
