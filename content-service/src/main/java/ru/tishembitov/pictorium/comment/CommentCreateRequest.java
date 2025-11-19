package ru.tishembitov.pictorium.comment;

import jakarta.validation.constraints.Size;

public record CommentCreateRequest(

        @Size(max = 400, message = "Content must not exceed 400 characters")
        String content,

        String imageId,
        String imageUrl
) {}