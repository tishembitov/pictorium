package ru.tishembitov.pictorium.like;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tishembitov.pictorium.comment.CommentResponse;
import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

public interface LikeService {

    PinResponse likePin(UUID pinId);
    void unlikePin(UUID pinId);
    Page<LikeResponse> getLikesOnPin(UUID pinId, Pageable pageable);

    CommentResponse likeComment(UUID commentId);
    void unlikeComment(UUID commentId);
    Page<LikeResponse> getLikesOnComment(UUID commentId, Pageable pageable);



}
