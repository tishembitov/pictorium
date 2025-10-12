package ru.tishembitov.pictorium.subscription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.tishembitov.pictorium.user.UserResponse;

public interface SubscriptionService {
    SubscriptionResponse followUser(String id, String userIdToFollow);

    void unfollowUser(String id, String userIdToUnfollow);

    FollowCheckResponse checkUserFollow(String id, String userIdToCheck);

    Page<UserResponse> getFollowing(String userId, Pageable pageable);

    Page<UserResponse> getFollowers(String userId, Pageable pageable);


}
