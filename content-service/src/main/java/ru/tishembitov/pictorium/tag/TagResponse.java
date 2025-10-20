package ru.tishembitov.pictorium.tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;


public record TagResponse(
        UUID id,
        @NotBlank(message = "Tag name cannot be blank")
        @Size(max = 100, message = "Tag name must not exceed 100 characters")
        String name
) {}