package ru.tishembitov.pictorium.image;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.time.Instant;

@Entity
@Table(name = "image_records", indexes = {
        @Index(name = "idx_image_id", columnList = "imageId"),
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Image {

    @Id
    @Column(unique = true, nullable = false)
    private String id;

    @Column(nullable = false)
    private String objectName;

    @Column(nullable = false)
    private String bucketName;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageStatus status;

    // Связь с thumbnail
    private String thumbnailImageId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant confirmedAt;

    public enum ImageStatus {
        PENDING,    // Presigned URL выдан, ожидает загрузки
        CONFIRMED,  // Загрузка подтверждена
        EXPIRED,    // Presigned URL истёк без загрузки
        DELETED     // Удалено
    }
}
