package ru.tishembitov.pictorium.comment;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.tishembitov.pictorium.pin.Pin;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id")
    private Pin pin;

    @Column(nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment parentComment;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer replyCount = 0;

    @Column(length = 400)
    private String content;

    @Column(length = 100)
    private String imageId;  // ID в storage-service

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}