package ru.tishembitov.pictorium.notification;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipientId"),
        @Index(name = "idx_notification_recipient_status", columnList = "recipientId, status"),
        @Index(name = "idx_notification_recipient_created", columnList = "recipientId, createdAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String recipientId;

    @Column(nullable = false)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    // Reference IDs
    private UUID referenceId;      // pinId, commentId, chatId, etc.
    private UUID secondaryRefId;   // messageId (for chat), parentCommentId (for replies)

    // Preview content
    @Column(length = 200)
    private String previewText;

    @Column(length = 200)
    private String previewImageId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant readAt;
}