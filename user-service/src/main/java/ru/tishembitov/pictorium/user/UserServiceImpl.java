package ru.tishembitov.pictorium.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.tishembitov.pictorium.exception.UsernameAlreadyExistsException;
import ru.tishembitov.pictorium.exception.UserNotFoundException;
import ru.tishembitov.pictorium.file.FileStorageService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FileStorageService fileStorageService;

    @Override
    public UserResponse getUserById(String id) {
        log.info("Fetching user by ID: {}", id);
        User user = getUserByIdOrThrow(id);
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
    @Transactional
    public UserResponse uploadBannerImage(String id, MultipartFile file) {
        User user = getUserByIdOrThrow(id);

        if (user.getBannerImage() != null) {
            fileStorageService.deleteFile(user.getBannerImage());
        }

        String bannerPath = fileStorageService.saveFile(file, "banner");
        user.setBannerImage(bannerPath);

        User updatedUser = userRepository.save(user);
        log.info("Banner image uploaded successfully for user: {}", id);

        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse uploadProfileImage(String id, MultipartFile file) {
        User user = getUserByIdOrThrow(id);

        if (user.getImage() != null) {
            fileStorageService.deleteFile(user.getImage());
        }

        String imagePath = fileStorageService.saveFile(file, "profile");
        user.setImage(imagePath);

        User updatedUser = userRepository.save(user);
        log.info("Profile image uploaded successfully for user: {}", id);

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
