package ru.tishembitov.pictorium.comment;


import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tishembitov.pictorium.pin.Pin;
import ru.tishembitov.pictorium.pin.PinResponse;

import java.util.UUID;

public interface CommentService {


    CommentResponse createCommentOnPin(UUID pinId, @Valid CommentCreateRequest request);

    Page<CommentResponse> getCommentsOnPin(UUID pinId, Pageable pageable);

    CommentResponse getCommentById(UUID commentId);

    CommentResponse updateComment(UUID commentId, @Valid CommentUpdateRequest request);

    void deleteComment(UUID commentId);

    CommentResponse createReplyOnComment(UUID commentId, @Valid CommentCreateRequest request);

    Page<CommentResponse> getRepliesOnComment(UUID commentId, Pageable pageable);

    CommentResponse buildCommentResponse(Comment comment, Boolean isLiked);
}
