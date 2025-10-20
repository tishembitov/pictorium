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
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PinResponse> likePin(@PathVariable UUID pinId) {
        return ResponseEntity.ok(likeService.likePin(pinId));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> unlikePin(@PathVariable UUID pinId) {
        likeService.unlikePin(pinId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<LikeResponse>> getLikes(
            @PathVariable UUID pinId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(likeService.getLikesOnPin(pinId, pageable));
    }
}
