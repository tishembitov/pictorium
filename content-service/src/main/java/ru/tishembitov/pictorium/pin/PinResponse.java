package ru.tishembitov.pictorium.pin;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PinResponse (
     UUID id,
     UUID userId,
     String title,
     String description,
     String href,
     String image,
     String videoPreview,
     String rgb,
     String height,
     Instant createdAt,
     Instant updatedAt,
     Set<String> tags,

     boolean isLiked,
     boolean isSaved,
     long savesCount,
     long commentsCount,
     long likesCount
) {}
