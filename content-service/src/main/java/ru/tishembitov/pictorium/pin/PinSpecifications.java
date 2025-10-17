package ru.tishembitov.pictorium.pin;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import ru.tishembitov.pictorium.like.Like;
import ru.tishembitov.pictorium.savedPins.SavedPin;
import ru.tishembitov.pictorium.tag.Tag;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PinSpecifications {

    public static Specification<Pin> build(PinFilter filter) {
        if (filter == null) {
            return null;
        }

        var specs = Stream.of(
                        byTextSearch(filter.q()),
                        byTags(filter.tags()),
                        byAuthor(filter.authorId()),
                        bySavedBy(filter.savedBy()),
                        byLikedBy(filter.likedBy()),
                        byCreatedFrom(filter.createdFrom()),
                        byCreatedTo(filter.createdTo()),
                        byRelatedTo(filter.relatedTo()),
                        needsDistinct(filter) ? distinct() : null
                )
                .filter(Objects::nonNull)
                .toList();

        return specs.isEmpty() ? null : Specification.allOf(specs);
    }

    private static Specification<Pin> byTextSearch(String query) {
        return (root, criteriaQuery, cb) -> {
            if (query == null || query.isBlank()) cb.conjunction();
            String searchPattern = "%" + query.toLowerCase() + "%";
            var titleLike = cb.like(cb.lower(root.get("title")), searchPattern);
            var descLike = cb.like(cb.lower(root.get("description")), searchPattern);
            return cb.or(titleLike, descLike);
        };
    }

    public static Specification<Pin> byTags(Set<String> tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.isEmpty()) return cb.conjunction();
            var lower = tags.stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            if (lower.isEmpty()) return cb.conjunction();

            Join<Pin, Tag> tagJoin = root.join("tags", JoinType.INNER);
            query.distinct(true);
            return cb.lower(tagJoin.get("name")).in(lower);
        };
    }

    private static Specification<Pin> byAuthor(String authorId) {
        return (root, query, cb) -> {
            if (authorId == null) cb.conjunction();
            return cb.equal(root.get("authorId"), authorId);
        };
    }

    public static Specification<Pin> bySavedBy(String userId) {
        return (root, query, cb) -> {
            if (userId == null) return cb.conjunction();
            Join<Pin, SavedPin> saves = root.join("savedByUsers", JoinType.INNER);
            return cb.equal(saves.get("userId"), userId);
        };
    }

    public static Specification<Pin> byLikedBy(String userId) {
        return (root, query, cb) -> {
            if (userId == null) return cb.conjunction();
            Join<Pin, Like> likes = root.join("likes", JoinType.INNER);
            return cb.equal(likes.get("userId"), userId);
        };
    }

    private static Specification<Pin> byCreatedFrom(Instant createdFrom) {
        return (root, query, cb) -> {
            if (createdFrom == null) cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
        };
    }

    private static Specification<Pin> byCreatedTo(Instant createdTo) {
        return (root, query, cb) -> {
            if (createdTo == null) cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("createdAt"), createdTo);
        };
    }

    public static Specification<Pin> byRelatedTo(UUID pinId) {
        return (root, query, cb) -> {
            if (pinId == null) return cb.conjunction();
            query.distinct(true);

            Subquery<Tag> sq = query.subquery(Tag.class);
            Root<Pin> p = sq.from(Pin.class);
            Join<Pin, Tag> tSource = p.join("tags", JoinType.INNER);
            Join<Pin, Tag> tCandidate = root.join("tags", JoinType.INNER);

            sq.select(tSource)
                    .where(
                            cb.equal(p.get("id"), pinId),
                            cb.equal(tSource.get("id"), tCandidate.get("id"))
                    );

            return cb.and(cb.notEqual(root.get("id"), pinId), cb.exists(sq));
        };
    }

    private static Specification<Pin> distinct() {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.conjunction();
        };
    }

    private static boolean needsDistinct(PinFilter filter) {
        return filter.tags() != null ||
                filter.relatedTo() != null ||
                filter.savedBy() != null ||
                filter.likedBy() != null;
    }
}