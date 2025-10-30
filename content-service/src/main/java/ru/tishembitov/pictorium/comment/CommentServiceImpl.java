package ru.tishembitov.pictorium.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.BadRequestException;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.like.LikeRepository;
import ru.tishembitov.pictorium.pin.Pin;
import ru.tishembitov.pictorium.pin.PinRepository;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PinRepository pinRepository;
    private final CommentMapper commentMapper;
    //private final NotificationService notificationService;
    //TODO: сделать изменение счетчиков на n, а не единично + сделать в миграции liquabase каскадное удаление

    public CommentResponse createCommentOnPin(UUID pinId, CommentCreateRequest request) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        String currentUserId = SecurityUtils.requireCurrentUserId();

        Comment comment = commentMapper.toEntity(request, pin, currentUserId, null);
        comment = commentRepository.save(comment);

        pinRepository.incrementCommentCount(pinId);
        return commentMapper.toResponse(comment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsOnPin(UUID pinId, Pageable pageable) {
        if (!pinRepository.existsById(pinId)) {
            throw new ResourceNotFoundException("Pin not found with id: " + pinId);
        }

        Page<Comment> comments = commentRepository.findByPinIdOrderByCreatedAtDesc(pinId, pageable);

        return comments.map(comment -> commentMapper.toResponse(
                comment,
                SecurityUtils.getCurrentUserId()
                        .map(userId -> likeRepository.existsByUserIdAndCommentId(userId, comment.getId()))
                        .orElse(false)
        ));
    }

    public CommentResponse createReplyOnComment(UUID commentId, CommentCreateRequest request) {
        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (parentComment.getParentComment() != null) {
            throw new BadRequestException("Cannot reply to a reply");
        }

        String currentUserId = SecurityUtils.requireCurrentUserId();
        Pin pin = parentComment.getPin();

        Comment reply = commentMapper.toEntity(request, pin, currentUserId, parentComment);
        reply = commentRepository.save(reply);

        commentRepository.incrementReplyCount(commentId);
        pinRepository.incrementCommentCount(pin.getId());

        return commentMapper.toResponse(reply);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getRepliesOnComment(UUID commentId, Pageable pageable) {
        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment not found with id: " + commentId);
        }

        Page<Comment> replies = commentRepository
                .findByParentCommentIdOrderByCreatedAtDesc(commentId, pageable);

        return replies.map(comment -> commentMapper.toResponse(
                comment,
                SecurityUtils.getCurrentUserId()
                        .map(userId -> likeRepository.existsByUserIdAndCommentId(userId, commentId))
                        .orElse(false)
        ));
    }

    @Transactional(readOnly = true)
    public CommentResponse getCommentById(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        return commentMapper.toResponse(
                comment,
                SecurityUtils.getCurrentUserId()
                        .map(userId -> likeRepository.existsByUserIdAndCommentId(userId, commentId))
                        .orElse(false)
        );
    }

    public CommentResponse updateComment(UUID commentId, CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        String currentUserId = SecurityUtils.requireCurrentUserId();
        checkCommentOwnership(comment, currentUserId);

        if (request.content() != null) {
            comment.setContent(request.content());
        }

        if (request.imageUrl() != null) {
            comment.setImageUrl(request.imageUrl());
        }

        comment = commentRepository.save(comment);

        return commentMapper.toResponse(
                comment,
                likeRepository.existsByUserIdAndCommentId(currentUserId, commentId)
        );
    }

    public void deleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        String currentUserId = SecurityUtils.requireCurrentUserId();
        checkCommentOwnership(comment, currentUserId);

        UUID pinId = comment.getPin().getId();

        if (comment.getParentComment() != null) {
            commentRepository.decrementReplyCount(comment.getParentComment().getId());
            pinRepository.decrementCommentCount(pinId);
        } else {
            long replyCount = commentRepository.countByParentCommentId(commentId);
            long totalToDelete = 1 + replyCount;
            pinRepository.decrementCommentCountBy(pinId, totalToDelete);
        }

        commentRepository.delete(comment);
    }

    private void checkCommentOwnership(Comment comment, String userId) {
        if (!comment.getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to modify this comment");
        }
    }
}
