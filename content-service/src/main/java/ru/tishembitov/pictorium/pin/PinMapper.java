package ru.tishembitov.pictorium.pin;

import org.mapstruct.*;
import ru.tishembitov.pictorium.tag.Tag;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PinMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", source = "userId")
    @Mapping(target = "image", source = "request.imageUrl")
    @Mapping(target = "videoPreview", source = "request.videoPreviewUrl")
    @Mapping(target = "rgb", source = "request.rgb")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "pinLikes", ignore = true)
    @Mapping(target = "savedByUsers", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "saveCount", ignore = true)
    Pin toEntity(String userId, PinCreateRequest request);

    @Mapping(target = "userId", source = "pin.authorId")
    @Mapping(target = "tags", source = "pin.tags")
    @Mapping(target = "savesCount", source = "pin.saveCount")
    @Mapping(target = "commentsCount", source = "pin.commentCount")
    @Mapping(target = "likesCount", source = "pin.likeCount")
    PinResponse toResponse(Pin pin, boolean isLiked, boolean isSaved);

    default Set<String> mapTags(Set<Tag> tags) {
        if (tags == null) return Set.of();
        return tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "image", source = "imageUrl")
    @Mapping(target = "videoPreview", source = "videoPreviewUrl")
    @Mapping(target = "rgb", source = "rgb")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "pinLikes", ignore = true)
    @Mapping(target = "savedByUsers", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "saveCount", ignore = true)
    void updateEntity(@MappingTarget Pin pin, PinUpdateRequest request);
}