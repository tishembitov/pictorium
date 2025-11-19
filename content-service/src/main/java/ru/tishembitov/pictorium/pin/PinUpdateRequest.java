package ru.tishembitov.pictorium.pin;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PinUpdateRequest(

        String imageId,
        String imageUrl,
        String thumbnailId,
        String thumbnailUrl,

        String videoPreviewId,
        String videoPreviewUrl,

        @Size(max = 200)
        String title,

        @Size(max = 400)
        String description,

        @Size(max = 200)
        String href,

        @Size(max = 100)
        String rgb,

        Integer width,

        Integer height,

        Set<@NotBlank @Size(max = 100) String> tags
) {}
