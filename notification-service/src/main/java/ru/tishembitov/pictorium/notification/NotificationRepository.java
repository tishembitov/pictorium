package ru.tishembitov.pictorium.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdOrderByUpdatedAtDesc(
            String recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndStatusOrderByUpdatedAtDesc(
            String recipientId, NotificationStatus status, Pageable pageable);

    long countByRecipientIdAndStatus(String recipientId, NotificationStatus status);

    @Query("""
        SELECT n FROM Notification n 
        WHERE n.recipientId = :recipientId 
        AND n.type = 'NEW_MESSAGE' 
        AND n.referenceId = :chatId 
        AND n.status = 'UNREAD'
    """)
    Optional<Notification> findUnreadMessagesNotification(
            @Param("recipientId") String recipientId,
            @Param("chatId") UUID chatId
    );

    @Query("""
        SELECT n FROM Notification n 
        WHERE n.recipientId = :recipientId 
        AND n.type = :type 
        AND n.referenceId = :pinId 
        AND n.status = 'UNREAD'
    """)
    Optional<Notification> findUnreadPinNotification(
            @Param("recipientId") String recipientId,
            @Param("type") NotificationType type,
            @Param("pinId") UUID pinId
    );

    @Query("""
        SELECT n FROM Notification n 
        WHERE n.recipientId = :recipientId 
        AND n.type = 'COMMENT_LIKED' 
        AND n.referenceId = :commentId 
        AND n.status = 'UNREAD'
    """)
    Optional<Notification> findUnreadCommentLikeNotification(
            @Param("recipientId") String recipientId,
            @Param("commentId") UUID commentId
    );

    @Query("""
        SELECT n FROM Notification n 
        WHERE n.recipientId = :recipientId 
        AND n.type = 'COMMENT_REPLIED' 
        AND n.secondaryRefId = :parentCommentId 
        AND n.status = 'UNREAD'
    """)
    Optional<Notification> findUnreadRepliesNotification(
            @Param("recipientId") String recipientId,
            @Param("parentCommentId") UUID parentCommentId
    );

    @Query("""
        SELECT n FROM Notification n 
        WHERE n.recipientId = :recipientId 
        AND n.type = 'USER_FOLLOWED' 
        AND n.status = 'UNREAD'
    """)
    Optional<Notification> findUnreadFollowsNotification(
            @Param("recipientId") String recipientId
    );

    @Query("""
    SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END 
    FROM Notification n 
    WHERE n.recipientId = :recipientId 
    AND n.type = 'USER_FOLLOWED' 
    AND n.status = 'UNREAD'
    AND (n.actorId = :actorId OR :actorId MEMBER OF n.allActorIds)
""")
    boolean isActorAlreadyInFollowNotification(
            @Param("recipientId") String recipientId,
            @Param("actorId") String actorId
    );

    @Modifying
    @Query("""
        UPDATE Notification n 
        SET n.status = :newStatus, n.readAt = :readAt 
        WHERE n.recipientId = :recipientId AND n.status = :currentStatus
    """)
    int markAllAsRead(
            @Param("recipientId") String recipientId,
            @Param("currentStatus") NotificationStatus currentStatus,
            @Param("newStatus") NotificationStatus newStatus,
            @Param("readAt") Instant readAt
    );

    @Modifying
    @Query("""
        UPDATE Notification n 
        SET n.status = :newStatus, n.readAt = :readAt 
        WHERE n.id IN :ids AND n.recipientId = :recipientId
    """)
    int markAsRead(
            @Param("ids") List<UUID> ids,
            @Param("recipientId") String recipientId,
            @Param("newStatus") NotificationStatus newStatus,
            @Param("readAt") Instant readAt
    );

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
    int deleteOlderThan(@Param("threshold") Instant threshold);
}