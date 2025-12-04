package ru.tishembitov.pictorium.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.client.ImageService;
import ru.tishembitov.pictorium.exception.UsernameAlreadyExistsException;
import ru.tishembitov.pictorium.exception.UserNotFoundException;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ImageService imageService;

    @Override
    public UserResponse getUserById(String userId) {
        return userMapper.toResponse(getUserByIdOrThrow(userId));
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User user = getUserByIdOrThrow(id);

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new UsernameAlreadyExistsException("Username taken: " + request.username());
            }
        }

        handleImageUpdate(request.imageId(), user.getImageId(), user::setImageId);
        handleImageUpdate(request.bannerImageId(), user.getBannerImageId(), user::setBannerImageId);

        userMapper.updateFromRequest(request, user);

        return userMapper.toResponse(userRepository.save(user));
    }

    private void handleImageUpdate(String newId, String currentId, Consumer<String> setter) {
        if (newId == null) {
            return;
        }

        if (newId.isBlank()) {
            imageService.deleteImageSafely(currentId);
            setter.accept(null);
            return;
        }

        if (!newId.equals(currentId)) {
            imageService.deleteImageSafely(currentId);
            setter.accept(newId);
        }
    }

    @Override
    public User getUserByIdOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    @Override
    public void validateUserExists(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found: " + userId);
        }
    }
}