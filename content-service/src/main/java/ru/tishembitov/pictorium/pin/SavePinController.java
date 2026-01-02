package ru.tishembitov.pictorium.pin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.board.BoardService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pins")
@RequiredArgsConstructor
public class SavePinController {

    private final BoardService boardService;

    @PostMapping("/{pinId}/saves")
    public ResponseEntity<PinResponse> savePin(@PathVariable UUID pinId) {
        PinResponse response = boardService.savePin(pinId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{pinId}/saves")
    public ResponseEntity<Void> unsavePin(@PathVariable UUID pinId) {
        boardService.unsavePin(pinId);
        return ResponseEntity.noContent().build();
    }
}