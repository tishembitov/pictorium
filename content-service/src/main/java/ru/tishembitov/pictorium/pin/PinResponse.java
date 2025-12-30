package ru.tishembitov.pictorium.pin;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PinResponse(
        UUID id,
        String userId,
        String title,
        String description,
        String href,

        String imageId,
        String thumbnailId,
        String videoPreviewId,

        Instant createdAt,
        Instant updatedAt,
        Set<String> tags,

        Boolean isLiked,
        Boolean isSaved,
        String savedToBoardName,
        Integer savedToBoardCount,

        Integer saveCount,
        Integer commentCount,
        Integer likeCount,
        Integer viewCount,

        Integer originalWidth,
        Integer originalHeight,
        Integer thumbnailWidth,
        Integer thumbnailHeight
) {}