package ru.tishembitov.pictorium.pin;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PinCreateRequest(

        @NotBlank(message = "Image ID is required")
        String imageId,

        @Size(max = 64)
        String thumbnailId,

        @Size(max = 64)
        String videoPreviewId,

        @Size(max = 200)
        String title,

        @Size(max = 400)
        String description,

        @Size(max = 200)
        String href,

        Set<@NotBlank @Size(max = 100) String> tags,

        @NotNull(message = "Original width is required")
        @Min(1)
        Integer originalWidth,

        @NotNull(message = "Original height is required")
        @Min(1)
        Integer originalHeight,

        @NotNull(message = "Thumbnail width is required")
        @Min(1)
        Integer thumbnailWidth,

        @NotNull(message = "Thumbnail height is required")
        @Min(1)
        Integer thumbnailHeight
) {}