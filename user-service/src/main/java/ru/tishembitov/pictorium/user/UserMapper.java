package ru.tishembitov.pictorium.user;

import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "imageUrl", source = "imageUrl")
    @Mapping(target = "bannerImageUrl", source = "bannerImageUrl")
    UserResponse toResponseDto(User user, String imageUrl, String bannerImageUrl);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "recommendationCreatedAt", ignore = true)
    void updateUserFromUpdateDto(UserUpdateRequest userUpdateRequest, @MappingTarget User user);
}