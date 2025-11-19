package ru.tishembitov.pictorium.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @Column(unique = true, nullable = false)
    private String id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 100)
    private String imageId;  // ID в storage-service

    @Column(length = 500)
    private String imageUrl;  // Кешированный URL для производительности

    @Column(length = 100)
    private String bannerImageId;  // ID в storage-service

    @Column(length = 500)
    private String bannerImageUrl;  // Кешированный URL для производительности

    @Column(length = 200)
    private String description;

    private Instant recommendationCreatedAt;

    @Column(length = 100)
    private String instagram;

    @Column(length = 100)
    private String tiktok;

    @Column(length = 100)
    private String telegram;

    @Column(length = 100)
    private String pinterest;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}