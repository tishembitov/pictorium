package ru.tishembitov.pictorium.comment;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;


public record CommentCreateRequest(

        @Size(max = 400, message = "Content must not exceed 400 characters")
        String content,
        @URL(message = "Image must be a valid URL")
        String imageUrl
) {}
