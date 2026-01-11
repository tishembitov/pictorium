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
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Получить уведомления пользователя
     */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(
            String recipientId, Pageable pageable);

    /**
     * Получить непрочитанные уведомления
     */
    Page<Notification> findByRecipientIdAndStatusOrderByCreatedAtDesc(
            String recipientId, NotificationStatus status, Pageable pageable);

    /**
     * Подсчитать непрочитанные
     */
    long countByRecipientIdAndStatus(String recipientId, NotificationStatus status);

    /**
     * Пометить все как прочитанные
     */
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
            @Param("readAt") Instant readAt);

    /**
     * Пометить конкретные как прочитанные
     */
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
            @Param("readAt") Instant readAt);

    /**
     * Удалить старые уведомления
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
    int deleteOlderThan(@Param("threshold") Instant threshold);

    /**
     * Проверить существование уведомления (для дедупликации)
     */
    boolean existsByRecipientIdAndActorIdAndTypeAndReferenceId(
            String recipientId, String actorId, NotificationType type, UUID referenceId);
}