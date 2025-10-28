package ru.tishembitov.pictorium.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String userIdToFollow) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.followUser(jwt.getSubject(), userIdToFollow));
    }

    @DeleteMapping("/{userIdToUnfollow}")
    public ResponseEntity<Void> unfollowUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String userIdToUnfollow) {

        subscriptionService.unfollowUser(jwt.getSubject(), userIdToUnfollow);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check_user_follow/{userIdToCheck}")
    public ResponseEntity<FollowCheckResponse> checkUserFollow(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String userIdToCheck) {

        return ResponseEntity.ok(subscriptionService.checkUserFollow(jwt.getSubject(), userIdToCheck));
    }

    @GetMapping("/followers/{userId}")
    public ResponseEntity<Page<UserResponse>> getFollowers(
            @PathVariable String userId,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(subscriptionService.getFollowers(userId, pageable));
    }

    @GetMapping("/following/{userId}")
    public ResponseEntity<Page<UserResponse>> getFollowing(
            @PathVariable String userId,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(subscriptionService.getFollowing(userId, pageable));
    }
}