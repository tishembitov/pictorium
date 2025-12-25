package ru.tishembitov.pictorium.savedPin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.pin.PinResponse;
import ru.tishembitov.pictorium.util.SecurityUtils;

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
    
    @GetMapping("/saved/{userId}")
    public ResponseEntity<Page<PinResponse>> getSavedToProfilePins(
            @PathVariable String userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<PinResponse> pins = savedPinService.getSavedToProfilePins(userId, pageable);
        return ResponseEntity.ok(pins);
    }
    
    @GetMapping("/saved/me")
    public ResponseEntity<Page<PinResponse>> getMySavedToProfilePins(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        String currentUserId = SecurityUtils.requireCurrentUserId();
        Page<PinResponse> pins = savedPinService.getSavedToProfilePins(currentUserId, pageable);
        return ResponseEntity.ok(pins);
    }
}