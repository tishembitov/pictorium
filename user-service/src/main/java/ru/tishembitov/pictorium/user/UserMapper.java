package ru.tishembitov.pictorium.user;

import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    UserResponseDto toResponseDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "bannerImage", ignore = true)
    void updateUserFromUpdateDto(UserUpdateDto userUpdateDto, @MappingTarget User user);
}