package ru.tishembitov.pictorium.user;

import jakarta.validation.Valid;

public interface UserService {

    UserResponse getUserById(String userId);

    UserResponse getUserByUsername(String username);

    UserResponse updateUser(String id, @Valid UserUpdateRequest userUpdateRequest);

    User getUserByIdOrThrow(String id);

    void validateUserExists(String userId);

}
