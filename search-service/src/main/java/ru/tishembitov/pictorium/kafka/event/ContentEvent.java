package ru.tishembitov.pictorium.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEvent {

    private String type;
    private String actorId;
    private String actorUsername;
    private String recipientId;

    private UUID pinId;
    private String pinTitle;
    private String pinDescription;
    private Set<String> pinTags;
    private String imageId;
    private String thumbnailId;
    private Integer originalWidth;
    private Integer originalHeight;

    private Integer likeCount;
    private Integer saveCount;
    private Integer commentCount;
    private Integer viewCount;

    private UUID boardId;
    private String boardTitle;
    private Integer boardPinCount;

    private UUID commentId;
    private UUID secondaryRefId;

    private String previewText;
    private String previewImageId;

    @Builder.Default
    private Instant timestamp = Instant.now();
}