package ru.tishembitov.pictorium.savedPin;

import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

public interface SavedPinService {

    PinResponse saveToProfile(UUID pinId);

    void unsaveFromProfile(UUID pinId);
}
