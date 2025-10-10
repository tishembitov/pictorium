package ru.tishembitov.pictorium.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.tishembitov.pictorium.exception.UsernameAlreadyExistsException;
import ru.tishembitov.pictorium.exception.UserNotFoundException;
import ru.tishembitov.pictorium.file.FileStorageService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserSynchronizer userSynchronizer;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public UserResponseDto getCurrentUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        log.info("Fetching user by Keycloak ID: {}", keycloakId);
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> userSynchronizer.createUserFromToken(jwt));
        return userMapper.toResponseDto(user);
    }

    @Override
    public UserResponseDto getUserById(UUID id) {
        log.info("Fetching user by ID: {}", id);
        User user = getUserOrThrow(id);
        return userMapper.toResponseDto(user);
    }

    @Override
    public UserResponseDto getUserByUsername(String username) {
        log.info("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Jwt jwt, UserUpdateDto userUpdateDto) {
        User user = getUserOrThrow(getUserId(jwt));

        if (userUpdateDto.username() != null && !userUpdateDto.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(userUpdateDto.username())) {
                throw new UsernameAlreadyExistsException("Username '" + userUpdateDto.username() + "' is already taken");
            }
        }

        userMapper.updateUserFromUpdateDto(userUpdateDto, user);
        User updatedUser = userRepository.save(user);

        log.info("User updated successfully: {}", user.getId());
        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto uploadBannerImage(Jwt jwt, MultipartFile file) {
        User user = getUserOrThrow(getUserId(jwt));

        if (user.getBannerImage() != null) {
            fileStorageService.deleteFile(user.getBannerImage());
        }

        String bannerPath = fileStorageService.saveFile(file, "banner");
        user.setBannerImage(bannerPath);

        User updatedUser = userRepository.save(user);
        log.info("Banner image uploaded successfully for user: {}", user.getId());

        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto uploadProfileImage(Jwt jwt, MultipartFile file) {
        User user = getUserOrThrow(getUserId(jwt));

        if (user.getImage() != null) {
            fileStorageService.deleteFile(user.getImage());
        }

        String imagePath = fileStorageService.saveFile(file, "profile");
        user.setImage(imagePath);

        User updatedUser = userRepository.save(user);
        log.info("Profile image uploaded successfully for user: {}", user.getId());

        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    public void validateUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
    }

    @Override
    public User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    @Override
    public UUID getUserId(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("User not found with KeycloakId: " + keycloakId))
                .getId();
    }

    @Override
    public Page<UserResponseDto> toResponseDtoPage(Page<User> users) {
        return userMapper.toResponseDtoPage(users);
    }

}
