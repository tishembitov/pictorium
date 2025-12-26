package ru.tishembitov.pictorium.savedPin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pins")
@RequiredArgsConstructor
public class SavedPinController {

    private final SavedPinService savedPinService;
    
    @PostMapping("/{pinId}/save")
    public ResponseEntity<PinResponse> saveToProfile(@PathVariable UUID pinId) {
        PinResponse response = savedPinService.saveToProfile(pinId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @DeleteMapping("/{pinId}/save")
    public ResponseEntity<Void> unsaveFromProfile(@PathVariable UUID pinId) {
        savedPinService.unsaveFromProfile(pinId);
        return ResponseEntity.noContent().build();
    }
}