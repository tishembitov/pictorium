package ru.tishembitov.pictorium.pin;

import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import ru.tishembitov.pictorium.like.Like;
import ru.tishembitov.pictorium.savedPins.SavedPin;
import ru.tishembitov.pictorium.tag.Tag;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
            if (query == null || query.isBlank()) {
                return null;
            }
            String searchPattern = "%" + query.toLowerCase() + "%";
            var titleLike = cb.like(cb.lower(root.get("title")), searchPattern);
            var descLike = cb.like(cb.lower(root.get("description")), searchPattern);
            return cb.or(titleLike, descLike);
        };
    }

    private static Specification<Pin> byTags(Set<String> tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.isEmpty()) {
                return null;
            }
            Join<Pin, Tag> tagJoin = root.join("tags");
            return tagJoin.get("name").in(tags);
        };
    }

    private static Specification<Pin> byAuthor(String authorId) {
        return (root, query, cb) -> {
            if (authorId == null) {
                return null;
            }
            return cb.equal(root.get("authorId"), authorId);
        };
    }

    private static Specification<Pin> bySavedBy(String userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return null;
            }
            Join<Pin, SavedPin> savedJoin = root.join("savedByUsers");
            return cb.equal(savedJoin.get("userId"), userId);
        };
    }

    private static Specification<Pin> byLikedBy(String userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return null;
            }
            Join<Pin, Like> likeJoin = root.join("likes");
            return cb.equal(likeJoin.get("userId"), userId);
        };
    }

    private static Specification<Pin> byCreatedFrom(Instant createdFrom) {
        return (root, query, cb) -> {
            if (createdFrom == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
        };
    }

    private static Specification<Pin> byCreatedTo(Instant createdTo) {
        return (root, query, cb) -> {
            if (createdTo == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), createdTo);
        };
    }

    private static Specification<Pin> byRelatedTo(UUID pinId) {
        return (root, query, cb) -> {
            if (pinId == null) {
                return null;
            }

            var subquery = query.subquery(UUID.class);
            var subRoot = subquery.from(Pin.class);
            var tagJoin = subRoot.join("tags");
            subquery.select(tagJoin.get("id"))
                    .where(cb.equal(subRoot.get("id"), pinId));

            var currentTagJoin = root.join("tags");
            return cb.and(
                    currentTagJoin.get("id").in(subquery),
                    cb.notEqual(root.get("id"), pinId)
            );
        };
    }

    private static Specification<Pin> distinct() {
        return (root, query, cb) -> {
            query.distinct(true);
            return null;
        };
    }

    private static boolean needsDistinct(PinFilter filter) {
        return filter.tags() != null ||
                filter.relatedTo() != null ||
                filter.savedBy() != null ||
                filter.likedBy() != null;
    }
}