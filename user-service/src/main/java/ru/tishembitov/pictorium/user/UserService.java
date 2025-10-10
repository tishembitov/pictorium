package ru.tishembitov.pictorium.user;

import jakarta.validation.Valid;
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

    User getUserOrThrow(Jwt jwt);

    User getUserByIdOrThrow(UUID id);

    UUID getCurrentUserId(Jwt jwt);

    void validateUserExists(UUID userId);

}
