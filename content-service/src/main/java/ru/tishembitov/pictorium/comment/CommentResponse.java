package ru.tishembitov.pictorium.comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
         UUID id,
         UUID pinId,
         String userId,
         UUID parentCommentId,
         String content,
         String imageUrl,

         Boolean isLiked,
         Integer likeCount,
         Integer replyCount,
         Instant createdAt,
         Instant updatedAt
) {}
