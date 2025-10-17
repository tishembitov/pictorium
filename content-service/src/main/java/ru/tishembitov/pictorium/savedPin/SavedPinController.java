package ru.tishembitov.pictorium.savedPin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/pins")
@RequiredArgsConstructor
public class SavedPinController {

    private final SavedPinService savedPinService;

    @PostMapping("/{pinId}/saves")
    @ResponseStatus(HttpStatus.CREATED)
    public PinResponse save(@PathVariable UUID pinId) {
        return savedPinService.savePin(pinId);
    }

    @DeleteMapping("/{pinId}/saves")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@PathVariable UUID pinId) {
        savedPinService.unsavePin(pinId);
    }
}
