package ru.tishembitov.pictorium.pin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PinFilter(
        String q,
        List<String> tags,
        UUID authorId,
        UUID savedBy,
        UUID likedBy,
        UUID relatedTo,
        Instant createdFrom,
        Instant createdTo,
        Scope scope
) {}