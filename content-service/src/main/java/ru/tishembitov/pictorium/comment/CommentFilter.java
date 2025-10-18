package ru.tishembitov.pictorium.comment;

import java.util.UUID;

public record CommentFilter(
    UUID pinId,
    UUID parentCommentId,
    String userId
) {}
