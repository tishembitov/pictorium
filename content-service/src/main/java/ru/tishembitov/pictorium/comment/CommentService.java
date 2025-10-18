package ru.tishembitov.pictorium.comment;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {
    CommentResponse createComment(UUID pinId, UUID parentCommentId, @Valid CommentRequest request);

    Page<CommentResponse> getComments(CommentFilter filter, Pageable pageable);

    CommentResponse getCommentById(UUID commentId);

    CommentResponse updateComment(UUID commentId, @Valid CommentRequest request);

    void deleteComment(UUID commentId);

}
