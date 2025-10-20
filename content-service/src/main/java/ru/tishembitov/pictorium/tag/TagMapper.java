package ru.tishembitov.pictorium.tag;

import org.mapstruct.*;
import ru.tishembitov.pictorium.pin.Pin;
import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TagMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    TagResponse toResponse(Tag tag);

    List<TagResponse> toResponseList(List<Tag> tags);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "videoPreviewUrl", source = "videoPreviewUrl")
    CategoryResponse.PinPreview toPinPreview(Pin pin);

    @Mapping(target = "tagId", source = "tag.id")
    @Mapping(target = "tagName", source = "tag.name")
    @Mapping(target = "pin", source = "pin")
    CategoryResponse toCategoryResponse(Tag tag, Pin pin);

    default CategoryResponse toEverythingCategory(Pin lastPin) {
        if (lastPin == null) {
            return null;
        }
        return new CategoryResponse(
                null,
                "Everything",
                toPinPreview(lastPin)
        );
    }

    default CategoryResponse toCategory(Tag tag) {
        if (tag == null || tag.getPins().isEmpty()) {
            return null;
        }

        Pin firstPin = tag.getPins().stream()
                .findFirst()
                .orElse(null);

        if (firstPin == null) {
            return null;
        }

        return toCategoryResponse(tag, firstPin);
    }
}