package ru.tishembitov.pictorium.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :commentId")
    void incrementReplyCount(@Param("commentId") UUID commentId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount - 1 WHERE c.id = :commentId AND c.replyCount > 0")
    void decrementReplyCount(@Param("commentId") UUID commentId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :commentId")
    void incrementLikeCount(@Param("commentId") UUID commentId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :commentId AND c.likeCount > 0")
    void decrementLikeCount(@Param("commentId") UUID commentId);

    long countByParentCommentId(UUID parentCommentId);

    Page<Comment> findByParentCommentIdOrderByCreatedAtDesc(UUID parentCommentId, Pageable pageable);

    Page<Comment> findByPinIdOrderByCreatedAtDesc(UUID pinId, Pageable pageable);

    @Query("SELECT c.imageId FROM Comment c WHERE c.pin.id = :pinId AND c.imageId IS NOT NULL")
    List<String> findImageIdsByPinId(@Param("pinId") UUID pinId);

    @Query("SELECT c.imageId FROM Comment c WHERE c.parentComment.id = :parentId AND c.imageId IS NOT NULL")
    List<String> findImageIdsByParentId(@Param("parentId") UUID parentId);

}
