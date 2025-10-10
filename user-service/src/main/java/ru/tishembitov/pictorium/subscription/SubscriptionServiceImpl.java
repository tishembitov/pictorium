package ru.tishembitov.pictorium.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.BadRequestException;
import ru.tishembitov.pictorium.exception.SubscriptionAlreadyExistsException;
import ru.tishembitov.pictorium.exception.SubscriptionNotFoundException;
import ru.tishembitov.pictorium.user.*;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService{

    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;
    private final UserMapper userMapper;
    // private final UpdateService updateService; // Для создания уведомлений

    @Transactional
    public SubscriptionResponseDto followUser(Jwt jwt, UUID userIdToFollow) {

        User currentUser = userService.getUserOrThrow(jwt);
        UUID currentUserId = currentUser.getId();

        if (currentUserId.equals(userIdToFollow)) {
            throw new BadRequestException("User cannot follow themselves");
        }

        User userToFollow = userService.getUserByIdOrThrow(userIdToFollow);

        if (subscriptionRepository.existsByFollowerIdAndFollowingId(currentUserId, userIdToFollow)) {
            throw new SubscriptionAlreadyExistsException("Already following this user");
        }

        Subscription subscription = Subscription.builder()
                .follower(currentUser)
                .following(userToFollow)
                .build();

        subscriptionRepository.save(subscription);

        // TODO: Асинхронно создать уведомление (аналог make_update_follow.delay)
        // updateService.createFollowUpdate(userIdToFollow, currentUserId);

        log.info("User {} followed user {}", currentUserId, userIdToFollow);
        return new SubscriptionResponseDto("ok");
    }

    @Transactional
    public void unfollowUser(Jwt jwt, UUID userIdToUnfollow) {
        UUID currentUserId = userService.getCurrentUserId(jwt);

        if (!subscriptionRepository.existsByFollowerIdAndFollowingId(currentUserId, userIdToUnfollow)) {
            throw new SubscriptionNotFoundException("Subscription not found");
        }

        subscriptionRepository.deleteByFollowerIdAndFollowingId(currentUserId, userIdToUnfollow);

        log.info("User {} unfollowed user {}", currentUserId, userIdToUnfollow);
    }

    public FollowCheckResponseDto checkUserFollow(Jwt jwt, UUID userIdToCheck) {
        UUID currentUserId = userService.getCurrentUserId(jwt);

        boolean isFollowing = subscriptionRepository.existsByFollowerIdAndFollowingId(
                currentUserId, userIdToCheck);

        return new FollowCheckResponseDto(isFollowing);
    }

    public Page<UserResponseDto> getFollowers(UUID userId, Pageable pageable) {
        userService.validateUserExists(userId);
        Page<User> followersPage = subscriptionRepository.findFollowersByUserId(userId, pageable);

        return userMapper.toResponseDtoPage(followersPage);
    }

    public Page<UserResponseDto> getFollowing(UUID userId, Pageable pageable) {
        userService.validateUserExists(userId);
        Page<User> followingPage = subscriptionRepository.findFollowingByUserId(userId, pageable);

        return userMapper.toResponseDtoPage(followingPage);
    }
}