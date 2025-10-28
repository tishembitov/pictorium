package ru.tishembitov.pictorium.user;

import org.mapstruct.*;
import org.springframework.data.domain.Page;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    UserResponse toResponseDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "bannerImageUrl", ignore = true)
    void updateUserFromUpdateDto(UserUpdateRequest userUpdateRequest, @MappingTarget User user);

    default Page<UserResponse> toResponseDtoPage(Page<User> userPage) {
        return userPage.map(this::toResponseDto);
    }
}