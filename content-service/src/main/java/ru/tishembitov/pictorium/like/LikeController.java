package ru.tishembitov.pictorium.like;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pins/{pinId}/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PinResponse likePin(@PathVariable UUID pinId) {
        return likeService.likePin(pinId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikePin(@PathVariable UUID pinId) {
        likeService.unlikePin(pinId);
    }
}
