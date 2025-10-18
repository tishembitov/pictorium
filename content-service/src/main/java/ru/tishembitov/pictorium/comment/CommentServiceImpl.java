package ru.tishembitov.pictorium.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.AccessDeniedException;
import ru.tishembitov.pictorium.exception.BadRequestException;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.pin.Pin;
import ru.tishembitov.pictorium.pin.PinRepository;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService{

    private final CommentRepository commentRepository;
    private final PinRepository pinRepository;
    private final CommentMapper commentMapper;
    // private final NotificationService notificationService;

    public CommentResponse createComment(UUID pinId, UUID parentCommentId, CommentRequest request) {

        if (pinId == null && parentCommentId == null) {
            throw new BadRequestException("Either pinId or parentCommentId must be provided");
        }

        String currentUserId = SecurityUtils.requireCurrentUserId();

        Comment comment = Comment.builder()
                .userId(currentUserId)
                .content(request.content())
                .imageUrl(request.imageUrl())
                .build();

        Pin pin = null;
        Comment parentComment = null;

        if (pinId != null) {
            pin = pinRepository.findById(pinId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));
            comment.setPin(pin);
        }

        if (parentCommentId != null) {
            parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found with id: " + parentCommentId));
            comment.setParentComment(parentComment);
            commentRepository.incrementReplyCount(parentCommentId);

            if (pinId == null && parentComment.getPin() != null) {
                comment.setPin(parentComment.getPin());
                pin = parentComment.getPin();
            }
        }

        Comment savedComment = commentRepository.save(comment);

        if (pin != null) {
            pinRepository.incrementCommentCount(pin.getId());
        }

        //Todo
        if (parentComment != null && !parentComment.getUserId().equals(currentUserId)) {
            log.info("Sending reply notification to user: {}", parentComment.getUserId());
            // notificationService.sendReplyNotification(parentComment, savedComment);
        } else if (pin != null && !pin.getAuthorId().equals(currentUserId)) {
            log.info("Sending comment notification to user: {}", pin.getAuthorId());
            // notificationService.sendCommentNotification(pin, savedComment);
        }

        return commentMapper.toResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(CommentFilter filter, Pageable pageable) {
        Specification<Comment> spec = CommentSpecifications.withFilter(filter);
        Page<Comment> commentPage = commentRepository.findAll(spec, pageable);
        return commentPage.map(commentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CommentResponse getCommentById(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        return commentMapper.toResponse(comment);
    }

    @Override
    public CommentResponse updateComment(UUID commentId, CommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        String currentUserId = SecurityUtils.requireCurrentUserId();
        checkCommentOwnership(comment, currentUserId);

        if (request.content() != null) {
            comment.setContent(request.content());
        }
        if (request.imageUrl() != null) {
            comment.setImageUrl(request.imageUrl());
        }

        Comment updatedComment = commentRepository.save(comment);
        return commentMapper.toResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        String currentUserId = SecurityUtils.requireCurrentUserId();
        checkCommentOwnership(comment, currentUserId);

        if (comment.getParentComment() != null) {
            commentRepository.decrementReplyCount(comment.getParentComment().getId());
        }

        if (comment.getPin() != null) {
            pinRepository.decrementCommentCount(comment.getPin().getId());
        }

        commentRepository.delete(comment);
    }

    private void checkCommentOwnership(Comment comment, String userId) {
        if (!comment.getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to modify this comment");
        }
    }
}
