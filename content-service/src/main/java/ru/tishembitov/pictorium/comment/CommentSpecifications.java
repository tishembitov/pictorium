package ru.tishembitov.pictorium.comment;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommentSpecifications {

    public static Specification<Comment> withFilter(CommentFilter filter) {
        List<Specification<Comment>> specs = new ArrayList<>();

        if (filter.pinId() != null) {
            specs.add(hasPinId(filter.pinId()));
        }

        if (filter.parentCommentId() != null) {
            specs.add(hasParentCommentId(filter.parentCommentId()));
        }

        if (filter.userId() != null) {
            specs.add(hasUserId(filter.userId()));
        }


        return Specification.allOf(specs);
    }

    public static Specification<Comment> hasPinId(UUID pinId) {
        return (root, query, cb) ->
                cb.equal(root.get("pin").get("id"), pinId);
    }

    public static Specification<Comment> hasParentCommentId(UUID parentCommentId) {
        return (root, query, cb) ->
                cb.equal(root.get("parentComment").get("id"), parentCommentId);
    }

    public static Specification<Comment> hasUserId(String userId) {
        return (root, query, cb) ->
                cb.equal(root.get("userId"), userId);
    }

}