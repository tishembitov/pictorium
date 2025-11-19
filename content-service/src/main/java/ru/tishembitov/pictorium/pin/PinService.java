package ru.tishembitov.pictorium.pin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface PinService {

    PinResponse getPinById(UUID pinId);

    Page<PinResponse> findPins(PinFilter filter, Pageable pageable);

    PinResponse createPin(PinCreateRequest request);

    PinResponse updatePin(UUID pinId, PinUpdateRequest request);

    void deletePin(UUID pinId);

    Map<UUID, PinInteractionDto> getPinInteractionDtosBatch(Set<UUID> pinIds);

    PinResponse buildPinResponse(Pin pin, Boolean isLiked, Boolean isSaved);

}
