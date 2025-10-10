package ru.tishembitov.pictorium.subscription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.tishembitov.pictorium.user.UserResponseDto;

import java.util.UUID;

public interface SubscriptionService {
    SubscriptionResponseDto followUser(Jwt jwt, UUID userIdToFollow);

    void unfollowUser(Jwt jwt, UUID userIdToUnfollow);

    FollowCheckResponseDto checkUserFollow(Jwt jwt, UUID userIdToCheck);

    Page<UserResponseDto> getFollowing(UUID userId, Pageable pageable);

    Page<UserResponseDto> getFollowers(UUID userId, Pageable pageable);


}
