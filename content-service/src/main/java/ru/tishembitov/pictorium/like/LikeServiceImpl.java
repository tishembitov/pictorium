package ru.tishembitov.pictorium.like;

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
import ru.tishembitov.pictorium.pin.*;
import ru.tishembitov.pictorium.util.SecurityUtils;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final LikeRepository SavedPinRepository; // TODO: переименовать в savedPinRepository и использовать правильный тип
    private final PinRepository pinRepository;
    private final CommentRepository commentRepository;
    private final PinMapper pinMapper;
    private final CommentMapper commentMapper;
    private final LikeMapper likeMapper;

    @Override
    public PinResponse likePin(UUID pinId) {
        String userId = SecurityUtils.requireCurrentUserId();

        Pin pin = pinRepository.findByIdWithTags(pinId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pin with id " + pinId + " not found"));

        boolean isSaved = SavedPinRepository.existsByUserIdAndPinId(userId, pinId);

        if (likeRepository.existsByUserIdAndPinId(userId, pinId)) {
            return pinMapper.toResponse(pin, true, isSaved);
        }

        Like like = likeMapper.toEntity(userId, pin);
        likeRepository.save(like);
        pinRepository.incrementLikeCount(pinId);

        return pinMapper.toResponse(pin, true, isSaved);
    }

    @Override
    public void unlikePin(UUID pinId) {
        String userId = SecurityUtils.requireCurrentUserId();

        if (!pinRepository.existsById(pinId)) {
            throw new ResourceNotFoundException("Pin with id " + pinId + " not found");
        }

        Like like = likeRepository.findByUserIdAndPinId(userId, pinId)
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));

        likeRepository.delete(like);
        pinRepository.decrementLikeCount(pinId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LikeResponse> getLikesOnPin(UUID pinId, Pageable pageable) {
        if (!pinRepository.existsById(pinId)) {
            throw new ResourceNotFoundException("Pin with id " + pinId + " not found");
        }

        Page<Like> likesPage = likeRepository.findByPinIdOrderByCreatedAtDesc(pinId, pageable);

        return likesPage.map(likeMapper::toResponse);
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

        // TODO: Send notification if comment.getUserId() != userId

        return commentMapper.toResponse(comment, true);
    }

    @Override
    public void unlikeComment(UUID commentId) {
        String userId = SecurityUtils.requireCurrentUserId();

        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment with id " + commentId + " not found");
        }

        Like like = likeRepository.findByUserIdAndCommentId(userId, commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));

        likeRepository.delete(like);
        commentRepository.decrementLikeCount(commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LikeResponse> getLikesOnComment(UUID commentId, Pageable pageable) {
        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment with id " + commentId + " not found");
        }

        Page<Like> likesPage = likeRepository.findByCommentIdOrderByCreatedAtDesc(
                commentId,
                pageable
        );

        return likesPage.map(likeMapper::toResponse);
    }
}
