package ru.tishembitov.pictorium.pin;

import lombok.With;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@With
public record PinFilter(
        String q,
        Set<String> tags,
        String authorId,
        String savedBy,
        String likedBy,
        UUID relatedTo,
        Instant createdFrom,
        Instant createdTo,
        Scope scope
) {}