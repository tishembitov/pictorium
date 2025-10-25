package ru.tishembitov.pictorium.board;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(
            @Valid @RequestBody BoardCreateRequest request) {
        BoardResponse response = boardService.createBoard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<BoardResponse>> getMyBoards() {
        List<BoardResponse> boards = boardService.getMyBoards();
        return ResponseEntity.ok(boards);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BoardResponse>> getUserBoards(@PathVariable String userId) {
        List<BoardResponse> boards = boardService.getUserBoards(userId);
        return ResponseEntity.ok(boards);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoardById(@PathVariable UUID boardId) {
        BoardResponse board = boardService.getBoardById(boardId);
        return ResponseEntity.ok(board);
    }

    @PostMapping("/{boardId}/pins/{pinId}")
    public ResponseEntity<Void> addPinToBoard(@PathVariable UUID boardId, @PathVariable UUID pinId) {
        boardService.addPinToBoard(boardId, pinId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{boardId}/pins/{pinId}")
    public ResponseEntity<Void> removePinFromBoard(@PathVariable UUID boardId, @PathVariable UUID pinId) {
        boardService.removePinFromBoard(boardId, pinId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{boardId}/pins")
    public ResponseEntity<Page<PinResponse>> getBoardPins(
            @PathVariable UUID boardId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PinResponse> pins = boardService.getBoardPins(boardId, pageable);
        return ResponseEntity.ok(pins);
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable UUID boardId) {
        boardService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }
}
