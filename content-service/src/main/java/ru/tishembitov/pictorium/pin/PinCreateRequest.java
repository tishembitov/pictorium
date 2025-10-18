package ru.tishembitov.pictorium.pin;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PinCreateRequest(
        @Size(max = 200) String title,
        @Size(max = 400) String description,
        @Size(max = 200) String href,
        @URL String imageUrl,
        @URL String videoPreviewUrl,
        @Size(max = 100) String rgb,
        @Size(max = 100) String height,
        Set<@NotBlank @Size(max = 100) String> tags
) {}
