package ru.tishembitov.pictorium.like;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.comment.Comment;
import ru.tishembitov.pictorium.comment.CommentMapper;
import ru.tishembitov.pictorium.comment.CommentRepository;
import ru.tishembitov.pictorium.comment.CommentResponse;
import ru.tishembitov.pictorium.exception.ResourceNotFoundException;
import ru.tishembitov.pictorium.kafka.ContentEvent;
import ru.tishembitov.pictorium.kafka.ContentEventPublisher;
import ru.tishembitov.pictorium.kafka.ContentEventType;
import ru.tishembitov.pictorium.pin.*;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final PinRepository pinRepository;
    private final CommentRepository commentRepository;
    private final PinMapper pinMapper;
    private final PinService pinService;
    private final CommentMapper commentMapper;
    private final LikeMapper likeMapper;
    private final EntityManager entityManager;
    private final ContentEventPublisher eventPublisher;

    @Override
    public PinResponse likePin(UUID pinId) {
        String userId = SecurityUtils.requireCurrentUserId();

        Pin pin = pinRepository.findByIdWithTags(pinId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pin with id " + pinId + " not found"));

        if (likeRepository.existsByUserIdAndPinId(userId, pinId)) {
            return pinMapper.toResponse(pin, pinService.getPinInteractionDto(pinId));
        }

        Like like = likeMapper.toEntity(userId, pin);
        likeRepository.save(like);

        pinRepository.incrementLikeCount(pinId);

        entityManager.flush();
        entityManager.refresh(pin);

        log.info("Pin liked: pinId={}, userId={}", pinId, userId);

        if (!pin.getAuthorId().equals(userId)) {
            eventPublisher.publish(ContentEvent.builder()
                    .type(ContentEventType.PIN_LIKED.name())
                    .actorId(userId)
                    .recipientId(pin.getAuthorId())
                    .pinId(pinId)
                    .previewText(pin.getTitle())
                    .previewImageId(pin.getThumbnailId())
                    .build());
        }

        return pinMapper.toResponse(pin, pinService.getPinInteractionDto(pinId));
    }

    @Override
    public PinResponse unlikePin(UUID pinId) {
        String userId = SecurityUtils.requireCurrentUserId();

        Like like = likeRepository.findByUserIdAndPinId(userId, pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));

        likeRepository.delete(like);

        pinRepository.decrementLikeCount(pinId);

        Pin pin = pinRepository.findByIdWithTags(pinId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pin with id " + pinId + " not found"));

        entityManager.flush();
        entityManager.refresh(pin);

        log.info("Pin unliked: pinId={}, userId={}", pinId, userId);

        return pinMapper.toResponse(pin, pinService.getPinInteractionDto(pinId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LikeResponse> getLikesOnPin(UUID pinId, Pageable pageable) {
        if (!pinRepository.existsById(pinId)) {
            throw new ResourceNotFoundException("Pin with id " + pinId + " not found");
        }

        return likeRepository.findByPinIdOrderByCreatedAtDesc(pinId, pageable)
                .map(likeMapper::toResponse);
    }

    @Override
    public CommentResponse likeComment(UUID commentId) {
        String userId = SecurityUtils.requireCurrentUserId();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comment with id " + commentId + " not found"));

        if (likeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            return commentMapper.toResponse(comment, true);
        }

        Like like = likeMapper.toEntity(userId, comment);
        likeRepository.save(like);

        commentRepository.incrementLikeCount(commentId);

        entityManager.flush();
        entityManager.refresh(comment);

        log.info("Comment liked: commentId={}, userId={}", commentId, userId);

        if (!comment.getUserId().equals(userId)) {
            eventPublisher.publish(ContentEvent.builder()
                    .type(ContentEventType.COMMENT_LIKED.name())
                    .actorId(userId)
                    .recipientId(comment.getUserId())
                    .pinId(comment.getPin().getId())
                    .commentId(commentId)
                    .previewText(truncate(comment.getContent(), 100))
                    .previewImageId(comment.getImageId())
                    .build());
        }

        return commentMapper.toResponse(comment, true);
    }

    @Override
    public CommentResponse unlikeComment(UUID commentId) {
        String userId = SecurityUtils.requireCurrentUserId();

        Like like = likeRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));

        likeRepository.delete(like);

        commentRepository.decrementLikeCount(commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comment with id " + commentId + " not found"));

        entityManager.flush();
        entityManager.refresh(comment);

        log.info("Comment unliked: commentId={}, userId={}", commentId, userId);

        return commentMapper.toResponse(comment, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LikeResponse> getLikesOnComment(UUID commentId, Pageable pageable) {
        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment with id " + commentId + " not found");
        }

        return likeRepository.findByCommentIdOrderByCreatedAtDesc(commentId, pageable)
                .map(likeMapper::toResponse);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}