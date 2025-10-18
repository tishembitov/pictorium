package ru.tishembitov.pictorium.pin;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import ru.tishembitov.pictorium.like.Like;
import ru.tishembitov.pictorium.savedPin.SavedPin;
import ru.tishembitov.pictorium.tag.Tag;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class PinSpecifications {


    public static Specification<Pin> withFilter(PinFilter filter) {
        if (filter == null) {
            return null;
        }

        List<Specification<Pin>> specs = new ArrayList<>();

        specs.add(fetchTags());

        if (filter.q() != null && !filter.q().isBlank()) {
            specs.add(byTextSearch(filter.q()));
        }

        if (filter.tags() != null && !filter.tags().isEmpty()) {
            specs.add(byTags(filter.tags()));
        }

        if (filter.authorId() != null) {
            specs.add(byAuthor(filter.authorId()));
        }

        if (filter.savedBy() != null) {
            specs.add(bySavedBy(filter.savedBy()));
        }

        if (filter.likedBy() != null) {
            specs.add(byLikedBy(filter.likedBy()));
        }

        if (filter.createdFrom() != null) {
            specs.add(byCreatedFrom(filter.createdFrom()));
        }

        if (filter.createdTo() != null) {
            specs.add(byCreatedTo(filter.createdTo()));
        }

        if (filter.relatedTo() != null) {
            specs.add(byRelatedTo(filter.relatedTo()));
        }

        if (needsDistinct(filter)) {
            specs.add(distinct());
        }

        return Specification.allOf(specs);
    }


    public static Specification<Pin> byTextSearch(String query) {
        return (root, criteriaQuery, cb) -> {
            String searchPattern = "%" + query.toLowerCase() + "%";
            var titleLike = cb.like(cb.lower(root.get("title")), searchPattern);
            var descLike = cb.like(cb.lower(root.get("description")), searchPattern);
            return cb.or(titleLike, descLike);
        };
    }


    public static Specification<Pin> byTags(Set<String> tags) {
        return (root, query, cb) -> {
            Set<String> lower = tags.stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            if (lower.isEmpty()) {
                return cb.conjunction();
            }

            Join<Pin, Tag> tagJoin = root.join("tags", JoinType.INNER);
            query.distinct(true);
            return cb.lower(tagJoin.get("name")).in(lower);
        };
    }


    public static Specification<Pin> fetchTags() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("tags", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }


    public static Specification<Pin> byAuthor(String authorId) {
        return (root, query, cb) -> cb.equal(root.get("authorId"), authorId);
    }


    public static Specification<Pin> bySavedBy(String userId) {
        return (root, query, cb) -> {
            Join<Pin, SavedPin> saves = root.join("savedByUsers", JoinType.INNER);
            return cb.equal(saves.get("userId"), userId);
        };
    }


    public static Specification<Pin> byLikedBy(String userId) {
        return (root, query, cb) -> {
            Join<Pin, Like> likes = root.join("likes", JoinType.INNER);
            return cb.equal(likes.get("userId"), userId);
        };
    }


    public static Specification<Pin> byCreatedFrom(Instant createdFrom) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }


    public static Specification<Pin> byCreatedTo(Instant createdTo) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    public static Specification<Pin> byRelatedTo(UUID pinId) {
        return (root, query, cb) -> {
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

            return cb.and(
                    cb.notEqual(root.get("id"), pinId),
                    cb.exists(sq)
            );
        };
    }

    public static Specification<Pin> distinct() {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.conjunction();
        };
    }

    public static boolean needsDistinct(PinFilter filter) {
        return filter.tags() != null ||
                filter.relatedTo() != null ||
                filter.savedBy() != null ||
                filter.likedBy() != null;
    }
}