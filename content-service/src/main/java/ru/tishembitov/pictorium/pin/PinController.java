package ru.tishembitov.pictorium.pin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pins")
@RequiredArgsConstructor
public class PinController {

    private final PinService pinService;

    @GetMapping
    public ResponseEntity<Page<PinResponse>> getPins(
            @ModelAttribute @Valid PinFilter filter,
            Pageable pageable
    ) {
        Page<PinResponse> page = pinService.findPins(filter, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PinResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(pinService.getPinById(id));
    }

    @PostMapping
    public ResponseEntity<PinResponse> create(@Valid @RequestBody PinCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pinService.createPin(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PinResponse> update(@PathVariable UUID id, @Valid @RequestBody PinUpdateRequest request) {
        return ResponseEntity.ok(pinService.updatePin(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        pinService.deletePin(id);
    }
}