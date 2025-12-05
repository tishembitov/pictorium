package ru.tishembitov.pictorium.pin;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.tishembitov.pictorium.tag.Tag;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "pins", indexes = {
        @Index(name = "idx_pins_author_created", columnList = "authorId, createdAt"),
        @Index(name = "idx_pins_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Pin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String authorId;

    @Column(length = 200)
    private String title;

    @Column(length = 400)
    private String description;

    @Column(length = 200)
    private String href;

    @Column(nullable = false, length = 100)
    private String imageId;

    @Column(length = 100)
    private String thumbnailId;

    @Column(length = 100)
    private String videoPreviewId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;


    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "pins_tags",
            joinColumns = @JoinColumn(name = "pin_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer saveCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;
}