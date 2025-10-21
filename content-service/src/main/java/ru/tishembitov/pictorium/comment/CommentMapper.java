package ru.tishembitov.pictorium.comment;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(target = "pinId", source = "pin.id")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "isLiked", constant = "false")
    CommentResponse toResponse(Comment comment);

    @Mapping(target = "pinId", source = "comment.pin.id")
    @Mapping(target = "parentCommentId", source = "comment.parentComment.id")
    @Mapping(target = "imageUrl", source = "comment.imageUrl")
    @Mapping(target = "isLiked", expression = "java(isLiked)")
    CommentResponse toResponse(Comment comment, Boolean isLiked);
}
