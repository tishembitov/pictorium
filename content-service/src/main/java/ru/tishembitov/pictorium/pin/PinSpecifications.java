package ru.tishembitov.pictorium.pin;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import ru.tishembitov.pictorium.board.Board;
import ru.tishembitov.pictorium.board.BoardPin;
import ru.tishembitov.pictorium.like.Like;
import ru.tishembitov.pictorium.tag.Tag;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class PinSpecifications {

    private PinSpecifications() {}

    public static Specification<Pin> withFilter(PinFilter filter) {
        if (filter == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        }

        List<Specification<Pin>> specs = new ArrayList<>();

        if (hasText(filter.q())) {
            specs.add(byTextSearch(filter.q()));
        }

        if (filter.tags() != null && !filter.tags().isEmpty()) {
            specs.add(byTags(filter.tags()));
        }

        if (hasText(filter.authorId())) {
            specs.add(byAuthor(filter.authorId()));
        }

        if (hasText(filter.savedBy())) {
            specs.add(bySavedByUser(filter.savedBy()));
        }

        if (filter.boardId() != null) {
            specs.add(byBoard(filter.boardId()));
        }

        if (hasText(filter.likedBy())) {
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

    public static Specification<Pin> bySavedByUser(String userId) {
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<BoardPin> boardPinRoot = subquery.from(BoardPin.class);
            Join<BoardPin, Board> boardJoin = boardPinRoot.join("board");

            subquery.select(boardPinRoot.get("pin").get("id"))
                    .where(cb.equal(boardJoin.get("userId"), userId));

            return root.get("id").in(subquery);
        };
    }

    public static Specification<Pin> byBoard(UUID boardId) {
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<BoardPin> boardPinRoot = subquery.from(BoardPin.class);

            subquery.select(boardPinRoot.get("pin").get("id"))
                    .where(cb.equal(boardPinRoot.get("board").get("id"), boardId));

            return root.get("id").in(subquery);
        };
    }
    public static Specification<Pin> byTextSearch(String query) {
        return (root, criteriaQuery, cb) -> {
            String searchPattern = "%" + query.toLowerCase().trim() + "%";
            Predicate titleLike = cb.like(cb.lower(root.get("title")), searchPattern);
            Predicate descLike = cb.like(cb.lower(root.get("description")), searchPattern);
            return cb.or(titleLike, descLike);
        };
    }

    public static Specification<Pin> byTags(Set<String> tags) {
        return (root, query, cb) -> {
            Set<String> normalizedTags = tags.stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.toLowerCase(Locale.ROOT).trim())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            if (normalizedTags.isEmpty()) {
                return cb.conjunction();
            }

            Join<Pin, Tag> tagJoin = root.join("tags", JoinType.INNER);
            return cb.lower(tagJoin.get("name")).in(normalizedTags);
        };
    }

    public static Specification<Pin> byAuthor(String authorId) {
        return (root, query, cb) -> cb.equal(root.get("authorId"), authorId);
    }

    public static Specification<Pin> byLikedBy(String userId) {
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<Like> likeRoot = subquery.from(Like.class);

            subquery.select(likeRoot.get("pin").get("id"))
                    .where(cb.equal(likeRoot.get("userId"), userId));

            return root.get("id").in(subquery);
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
            Subquery<UUID> tagSubquery = query.subquery(UUID.class);
            Root<Pin> sourcePin = tagSubquery.from(Pin.class);
            Join<Pin, Tag> sourceTags = sourcePin.join("tags", JoinType.INNER);

            tagSubquery.select(sourceTags.get("id"))
                    .where(cb.equal(sourcePin.get("id"), pinId));

            Join<Pin, Tag> candidateTags = root.join("tags", JoinType.INNER);

            return cb.and(
                    cb.notEqual(root.get("id"), pinId),
                    candidateTags.get("id").in(tagSubquery)
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
        if (filter == null) return false;
        return (filter.tags() != null && !filter.tags().isEmpty())
                || filter.relatedTo() != null;
    }

    private static boolean hasText(String str) {
        return str != null && !str.isBlank();
    }
}