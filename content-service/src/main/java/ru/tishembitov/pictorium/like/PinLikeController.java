package ru.tishembitov.pictorium.like;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pins/{pinId}/likes")
@RequiredArgsConstructor
public class PinLikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<PinResponse> likePin(@PathVariable UUID pinId) {
        PinResponse response = likeService.likePin(pinId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping
    public ResponseEntity<PinResponse> unlikePin(@PathVariable UUID pinId) {
        PinResponse response = likeService.unlikePin(pinId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<LikeResponse>> getLikes(
            @PathVariable UUID pinId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<LikeResponse> likes = likeService.getLikesOnPin(pinId, pageable);
        return ResponseEntity.ok(likes);
    }
}