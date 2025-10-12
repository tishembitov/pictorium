package ru.tishembitov.pictorium.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.tishembitov.pictorium.user.UserResponse;


@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{userIdToFollow}")
    public ResponseEntity<SubscriptionResponse> followUser(
            Authentication authentication,
            @PathVariable String userIdToFollow) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.followUser(authentication.getName(), userIdToFollow));
    }

    @DeleteMapping("/{userIdToUnfollow}")
    public ResponseEntity<Void> unfollowUser(
            Authentication authentication,
            @PathVariable String userIdToUnfollow) {

        subscriptionService.unfollowUser(authentication.getName(), userIdToUnfollow);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check_user_follow/{userIdToCheck}")
    public ResponseEntity<FollowCheckResponse> checkUserFollow(
             Authentication authentication,
            @PathVariable String userIdToCheck) {

        return ResponseEntity.ok(subscriptionService.checkUserFollow(authentication.getName(), userIdToCheck));
    }

    @GetMapping("/followers/{id}")
    public ResponseEntity<Page<UserResponse>> getFollowers(
            @PathVariable String id,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(subscriptionService.getFollowers(id, pageable));
    }

    @GetMapping("/following/{id}")
    public ResponseEntity<Page<UserResponse>> getFollowing(
            @PathVariable String id,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(subscriptionService.getFollowing(id, pageable));
    }
}