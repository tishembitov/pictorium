package ru.tishembitov.pictorium.user;

import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;


public interface UserService {

    UserResponse getUserById(String id);

    UserResponse getUserByUsername(String username);

    UserResponse updateUser(String id, @Valid UserUpdateRequest userUpdateRequest);

    UserResponse uploadBannerImage(String id, MultipartFile file);

    UserResponse uploadProfileImage(String id, MultipartFile file);

    User getUserByIdOrThrow(String id);

    void validateUserExists(String userId);

}
