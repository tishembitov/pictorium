package ru.tishembitov.pictorium.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tishembitov.pictorium.exception.BadRequestException;
import ru.tishembitov.pictorium.exception.SubscriptionAlreadyExistsException;
import ru.tishembitov.pictorium.exception.SubscriptionNotFoundException;
import ru.tishembitov.pictorium.user.*;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService{

    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;
    private final UserMapper userMapper;

    // private final UpdateService updateService; // Для создания уведомлений

    @Transactional
    public SubscriptionResponse followUser(String id, String userIdToFollow) {

        if (id.equals(userIdToFollow)) {
            throw new BadRequestException("User cannot follow themselves");
        }

        User currentUser = userService.getUserByIdOrThrow(id);
        User userToFollow = userService.getUserByIdOrThrow(userIdToFollow);

        if (subscriptionRepository.existsByFollowerIdAndFollowingId(id, userIdToFollow)) {
            throw new SubscriptionAlreadyExistsException("Already following this user");
        }

        Subscription subscription = Subscription.builder()
                .follower(currentUser)
                .following(userToFollow)
                .build();

        subscriptionRepository.save(subscription);

        // TODO: Асинхронно создать уведомление (аналог make_update_follow.delay)
        // updateService.createFollowUpdate(userIdToFollow, currentUserId);

        log.info("User {} followed user {}", id, userIdToFollow);
        return new SubscriptionResponse("ok");
    }

    @Transactional
    public void unfollowUser(String id, String userIdToUnfollow) {

        if (!subscriptionRepository.existsByFollowerIdAndFollowingId(id, userIdToUnfollow)) {
            throw new SubscriptionNotFoundException("Subscription not found");
        }

        subscriptionRepository.deleteByFollowerIdAndFollowingId(id, userIdToUnfollow);

        log.info("User {} unfollowed user {}", id, userIdToUnfollow);
    }

    public FollowCheckResponse checkUserFollow(String id, String userIdToCheck) {

        boolean isFollowing = subscriptionRepository.existsByFollowerIdAndFollowingId(id, userIdToCheck);

        return new FollowCheckResponse(isFollowing);
    }

    public Page<UserResponse> getFollowers(String userId, Pageable pageable) {
        userService.validateUserExists(userId);
        Page<User> followersPage = subscriptionRepository.findFollowersByUserId(userId, pageable);

        return followersPage.map(userMapper::toResponse);
    }

    public Page<UserResponse> getFollowing(String userId, Pageable pageable) {
        userService.validateUserExists(userId);
        Page<User> followingPage = subscriptionRepository.findFollowingByUserId(userId, pageable);

        return followingPage.map(userMapper::toResponse);
    }
}