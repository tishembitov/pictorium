package ru.tishembitov.pictorium.comment;

import org.mapstruct.*;
import ru.tishembitov.pictorium.pin.Pin;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(target = "pinId", source = "comment.pin.id")
    @Mapping(target = "parentCommentId", source = "comment.parentComment.id")
    @Mapping(target = "isLiked", source = "isLiked")
    CommentResponse toResponse(Comment comment, Boolean isLiked);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pin", source = "pin")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "parentComment", source = "parentComment")
    @Mapping(target = "content", source = "request.content")
    @Mapping(target = "imageId", source = "request.imageId")
    @Mapping(target = "likeCount", constant = "0")
    @Mapping(target = "replyCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Comment toEntity(CommentCreateRequest request, Pin pin, String userId, Comment parentComment);
}