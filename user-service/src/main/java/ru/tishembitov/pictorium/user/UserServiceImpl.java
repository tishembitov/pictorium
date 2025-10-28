package ru.tishembitov.pictorium.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.UsernameAlreadyExistsException;
import ru.tishembitov.pictorium.exception.UserNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getUserById(String userId) {
        log.info("Fetching user by ID: {}", userId);
        User user = getUserByIdOrThrow(userId);
        return userMapper.toResponseDto(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        log.info("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest userUpdateRequest) {
        User user = getUserByIdOrThrow(id);

        if (userUpdateRequest.username() != null && !userUpdateRequest.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(userUpdateRequest.username())) {
                throw new UsernameAlreadyExistsException("Username '" + userUpdateRequest.username() + "' is already taken");
            }
        }

        userMapper.updateUserFromUpdateDto(userUpdateRequest, user);
        User updatedUser = userRepository.save(user);

        log.info("User updated successfully: {}", id);
        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    public void validateUserExists(String id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with ID: " + id);
        }
    }

    @Override
    public User getUserByIdOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
    }
}
