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
    @Mapping(target = "imageId", source = "request.imageId")
    @Mapping(target = "imageUrl", source = "request.imageUrl")
    @Mapping(target = "thumbnailId", source = "request.thumbnailId")
    @Mapping(target = "thumbnailUrl", source = "request.thumbnailUrl")
    @Mapping(target = "videoPreviewId", source = "request.videoPreviewId")
    @Mapping(target = "videoPreviewUrl", source = "request.videoPreviewUrl")
    @Mapping(target = "title", source = "request.title")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "href", source = "request.href")
    @Mapping(target = "rgb", source = "request.rgb")
    @Mapping(target = "width", source = "request.width")
    @Mapping(target = "height", source = "request.height")
    @Mapping(target = "fileSize", source = "request.fileSize")
    @Mapping(target = "contentType", source = "request.contentType")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "saveCount", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    Pin toEntity(String userId, PinCreateRequest request);

    @Mapping(target = "userId", source = "pin.authorId")
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "thumbnailUrl", source = "thumbnailUrl")
    @Mapping(target = "videoPreviewUrl", source = "videoPreviewUrl")
    @Mapping(target = "tags", source = "pin.tags")
    @Mapping(target = "isLiked", source = "isLiked")
    @Mapping(target = "isSaved", source = "isSaved")
    PinResponse toResponse(Pin pin, String imageUrl, String thumbnailUrl, String videoPreviewUrl,
                           Boolean isLiked, Boolean isSaved);

    default Set<String> mapTags(Set<Tag> tags) {
        if (tags == null) return Set.of();
        return tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "saveCount", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    void updateEntity(@MappingTarget Pin pin, PinUpdateRequest request);
}