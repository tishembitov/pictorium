package ru.tishembitov.pictorium.comment;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(target = "pinId", source = "pin.id")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    CommentResponse toResponse(Comment comment);
}
