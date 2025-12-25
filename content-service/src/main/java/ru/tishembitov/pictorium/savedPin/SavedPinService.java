package ru.tishembitov.pictorium.savedPin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

public interface SavedPinService {

    PinResponse saveToProfile(UUID pinId);

    void unsaveFromProfile(UUID pinId);

    Page<PinResponse> getSavedToProfilePins(String userId, Pageable pageable);

    boolean isPinSavedToProfile(UUID pinId);
}
