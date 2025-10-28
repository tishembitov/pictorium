package ru.tishembitov.pictorium.selectedBoard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.board.BoardResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/boards/selected")
@RequiredArgsConstructor
public class SelectedBoardController {

    private final SelectedBoardService selectedBoardService;

    @PatchMapping
    public ResponseEntity<Void> select(@RequestParam UUID boardId) {
        selectedBoardService.selectBoard(boardId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<BoardResponse> getSelectedBoard() {
        return ResponseEntity.ok(selectedBoardService.getSelectedBoard());
    }

    @DeleteMapping
    public ResponseEntity<Void> disable() {
        selectedBoardService.disableBoard();
        return ResponseEntity.noContent().build();
    }
}
