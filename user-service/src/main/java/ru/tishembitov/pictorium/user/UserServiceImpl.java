package ru.tishembitov.pictorium.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.client.ImageUrlService;
import ru.tishembitov.pictorium.exception.UsernameAlreadyExistsException;
import ru.tishembitov.pictorium.exception.UserNotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ImageUrlService imageUrlService;

    @Override
    public UserResponse getUserById(String userId) {
        log.info("Fetching user by ID: {}", userId);
        User user = getUserByIdOrThrow(userId);
        return buildUserResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        log.info("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
        return buildUserResponse(user);
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

        if (userUpdateRequest.imageId() != null && !userUpdateRequest.imageId().equals(user.getImageId())) {
            if (user.getImageId() != null && !user.getImageId().isBlank()) {
                imageUrlService.deleteImageSafely(user.getImageId());
            }

            if (!userUpdateRequest.imageId().isBlank()) {
                imageUrlService.validateImageExists(userUpdateRequest.imageId());
                user.setImageId(userUpdateRequest.imageId());
                user.setImageUrl(userUpdateRequest.imageUrl());
            } else {
                user.setImageId(null);
                user.setImageUrl(null);
            }
        }

        if (userUpdateRequest.bannerImageId() != null && !userUpdateRequest.bannerImageId().equals(user.getBannerImageId())) {
            if (user.getBannerImageId() != null && !user.getBannerImageId().isBlank()) {
                imageUrlService.deleteImageSafely(user.getBannerImageId());
            }

            if (!userUpdateRequest.bannerImageId().isBlank()) {
                imageUrlService.validateImageExists(userUpdateRequest.bannerImageId());
                user.setBannerImageId(userUpdateRequest.bannerImageId());
                user.setBannerImageUrl(userUpdateRequest.bannerImageUrl());
            } else {
                user.setBannerImageId(null);
                user.setBannerImageUrl(null);
            }
        }

        userMapper.updateUserFromUpdateDto(userUpdateRequest, user);
        User updatedUser = userRepository.save(user);

        log.info("User updated successfully: {}", id);
        return buildUserResponse(updatedUser);
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

    @Override
    public UserResponse buildUserResponse(User user) {
        String imageUrl = user.getImageId() != null
                ? imageUrlService.getImageUrl(user.getImageId())
                : null;

        String bannerImageUrl = user.getBannerImageId() != null
                ? imageUrlService.getImageUrl(user.getBannerImageId())
                : null;

        return userMapper.toResponseDto(user, imageUrl, bannerImageUrl);
    }
}