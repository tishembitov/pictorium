package ru.tishembitov.pictorium.pin;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PinUpdateRequest(

        @Size(max = 64)
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

        Set<@NotBlank @Size(max = 100) String> tags
) {}
