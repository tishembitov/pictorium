package ru.tishembitov.pictorium.like;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.comment.CommentResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments/{commentId}/likes")
@RequiredArgsConstructor
public class CommentLikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<CommentResponse> likeComment(@PathVariable UUID commentId) {
        CommentResponse response = likeService.likeComment(commentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> unlikeComment(@PathVariable UUID commentId) {
        likeService.unlikeComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<LikeResponse>> getLikes(
            @PathVariable UUID commentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<LikeResponse> likes = likeService.getLikesOnComment(commentId, pageable);
        return ResponseEntity.ok(likes);
    }
}