package ru.tishembitov.pictorium.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.client.ImageService;
import ru.tishembitov.pictorium.exception.BadRequestException;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.kafka.ContentEvent;
import ru.tishembitov.pictorium.kafka.ContentEventPublisher;
import ru.tishembitov.pictorium.kafka.ContentEventType;
import ru.tishembitov.pictorium.like.LikeRepository;
import ru.tishembitov.pictorium.pin.Pin;
import ru.tishembitov.pictorium.pin.PinRepository;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PinRepository pinRepository;
    private final CommentMapper commentMapper;
    private final ImageService imageService;
    private final ContentEventPublisher eventPublisher;

    @Override
    public CommentResponse createCommentOnPin(UUID pinId, @Valid CommentCreateRequest request) {
        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Pin not found with id: " + pinId));

        String currentUserId = SecurityUtils.requireCurrentUserId();

        Comment comment = commentMapper.toEntity(request, pin, currentUserId, null);
        comment = commentRepository.save(comment);

        pinRepository.incrementCommentCount(pinId);

        if (!pin.getAuthorId().equals(currentUserId)) {
            eventPublisher.publish(ContentEvent.builder()
                    .type(ContentEventType.PIN_COMMENTED.name())
                    .actorId(currentUserId)
                    .recipientId(pin.getAuthorId())
                    .pinId(pinId)
                    .commentId(comment.getId())
                    .previewText(truncate(request.content()))
                    .previewImageId(pin.getThumbnailId())
                    .build());
        }

        return commentMapper.toResponse(comment, false);
    }

    @Override
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

    @Override
    public CommentResponse createReplyOnComment(UUID commentId, @Valid CommentCreateRequest request) {
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

        if (!parentComment.getUserId().equals(currentUserId)) {
            eventPublisher.publish(ContentEvent.builder()
                    .type(ContentEventType.COMMENT_REPLIED.name())
                    .actorId(currentUserId)
                    .recipientId(parentComment.getUserId())
                    .pinId(pin.getId())
                    .commentId(reply.getId())
                    .secondaryRefId(parentComment.getId())
                    .previewText(truncate(request.content()))
                    .previewImageId(request.imageId())
                    .build());
        }

        return commentMapper.toResponse(reply, false);
    }

    @Override
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
                        .map(userId -> likeRepository.existsByUserIdAndCommentId(userId, comment.getId()))
                        .orElse(false)
        ));
    }

    @Override
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

    @Override
    public CommentResponse updateComment(UUID commentId, @Valid CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        String currentUserId = SecurityUtils.requireCurrentUserId();
        checkCommentOwnership(comment, currentUserId);

        handleImageUpdate(request.imageId(), comment.getImageId(), comment::setImageId);

        if (request.content() != null) {
            comment.setContent(request.content());
        }

        comment = commentRepository.save(comment);

        return commentMapper.toResponse(
                comment,
                likeRepository.existsByUserIdAndCommentId(currentUserId, commentId)
        );
    }

    @Override
    public void deleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        String currentUserId = SecurityUtils.requireCurrentUserId();
        checkCommentOwnership(comment, currentUserId);

        UUID pinId = comment.getPin().getId();
        boolean isReply = comment.getParentComment() != null;

        if (isReply) {
            imageService.deleteImageSafely(comment.getImageId());

            commentRepository.decrementReplyCount(comment.getParentComment().getId());
            pinRepository.decrementCommentCount(pinId);
        } else {
            List<String> replyImageIds = commentRepository.findImageIdsByParentId(commentId);

            long replyCount = commentRepository.countByParentCommentId(commentId);
            long totalToDelete = 1 + replyCount;

            imageService.deleteImageSafely(comment.getImageId());
            replyImageIds.forEach(imageService::deleteImageSafely);

            pinRepository.decrementCommentCountBy(pinId, totalToDelete);
        }

        commentRepository.delete(comment);

        log.info("Comment deleted: {} by user: {}", commentId, currentUserId);
    }

    private void handleImageUpdate(String newId, String currentId, Consumer<String> setter) {
        if (newId == null) {
            return;
        }

        if (newId.isBlank()) {
            imageService.deleteImageSafely(currentId);
            setter.accept(null);
            return;
        }

        if (!newId.equals(currentId)) {
            imageService.deleteImageSafely(currentId);
            setter.accept(newId);
        }
    }

    private void checkCommentOwnership(Comment comment, String userId) {
        if (!comment.getUserId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to modify this comment");
        }
    }

    private String truncate(String text) {
        if (text == null) return null;
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}