package ru.tishembitov.pictorium.user;

import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserService {
    UserResponseDto getUserByKeycloakId(String keycloakId);

    UserResponseDto getUserById(UUID id);

    UserResponseDto getUserByUsername(String username);

    UserResponseDto updateUser(String subject, @Valid UserUpdateDto patchRequest);

    UserResponseDto uploadBannerImage(UUID id, MultipartFile file);

}
