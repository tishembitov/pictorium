package ru.tishembitov.pictorium.pin;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

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
) {

    public PinFilter withAuthorId(String authorId) {
        return new PinFilter(q, tags, authorId, savedBy, likedBy, relatedTo,
                createdFrom, createdTo, scope);
    }

    public PinFilter withSavedBy(String savedBy) {
        return new PinFilter(q, tags, authorId, savedBy, likedBy, relatedTo,
                createdFrom, createdTo, scope);
    }

    public PinFilter withLikedBy(String likedBy) {
        return new PinFilter(q, tags, authorId, savedBy, likedBy, relatedTo,
                createdFrom, createdTo, scope);
    }

    public PinFilter withRelatedTo(UUID relatedTo) {
        return new PinFilter(q, tags, authorId, savedBy, likedBy, relatedTo,
                createdFrom, createdTo, scope);
    }

}