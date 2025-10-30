package ru.tishembitov.pictorium.like;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.tishembitov.pictorium.comment.Comment;
import ru.tishembitov.pictorium.pin.Pin;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LikeMapper {

    @Mapping(target = "pinId", source = "pin.id")
    @Mapping(target = "commentId", source = "comment.id")
    LikeResponse toResponse(Like like);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "pin", source = "pin")
    @Mapping(target = "comment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Like toEntity(String userId, Pin pin);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "pin", ignore = true)
    @Mapping(target = "comment", source = "comment")
    @Mapping(target = "createdAt", ignore = true)
    Like toEntity(String userId, Comment comment);
}