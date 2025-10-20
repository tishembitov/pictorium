package ru.tishembitov.pictorium.like;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LikeMapper {

    @Mapping(target = "pinId", source = "pin.id")
    @Mapping(target = "commentId", source = "comment.id")
    LikeResponse toResponse(Like like);

}