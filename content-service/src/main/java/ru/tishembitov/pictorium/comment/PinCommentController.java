package ru.tishembitov.pictorium.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pins")
@RequiredArgsConstructor
public class PinCommentController {

    private final CommentService commentService;

    @PostMapping("/{pinId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID pinId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        CommentResponse response = commentService.createCommentOnPin(pinId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{pinId}/comments")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable UUID pinId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<CommentResponse> comments = commentService.getCommentsOnPin(pinId, pageable);
        return ResponseEntity.ok(comments);
    }
}
