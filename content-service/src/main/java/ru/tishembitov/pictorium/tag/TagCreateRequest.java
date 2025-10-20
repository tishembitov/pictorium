package ru.tishembitov.pictorium.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record TagCreateRequest(
        @NotNull(message = "Pin ID is required")
        UUID pinId,
        @NotEmpty(message = "At least one tag is required")
        Set<@NotBlank String> tags
) {}
