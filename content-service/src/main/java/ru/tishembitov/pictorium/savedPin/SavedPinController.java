package ru.tishembitov.pictorium.savedPin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pins/{pinId}/saves")
@RequiredArgsConstructor
public class SavedPinController {

    private final SavedPinService savedPinService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PinResponse> save(@PathVariable UUID pinId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPinService.savePin(pinId));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> unsave(@PathVariable UUID pinId) {
        savedPinService.unsavePin(pinId);
        return ResponseEntity.noContent().build();
    }
}
