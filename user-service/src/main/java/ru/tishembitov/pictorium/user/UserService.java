package ru.tishembitov.pictorium.user;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserService {

    UserResponseDto getCurrentUser(Jwt jwt);

    UserResponseDto getUserById(UUID id);

    UserResponseDto getUserByUsername(String username);

    UserResponseDto updateUser(Jwt jwt, @Valid UserUpdateDto userUpdateDto);

    UserResponseDto uploadBannerImage(Jwt jwt, MultipartFile file);

    UserResponseDto uploadProfileImage(Jwt jwt, MultipartFile file);

    User getUserOrThrow(UUID userId);

    UUID getUserId(Jwt jwt);

    void validateUserExists(UUID userId);

    Page<UserResponseDto> toResponseDtoPage(Page<User> users);
}
