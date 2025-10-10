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
import ru.tishembitov.pictorium.user.UserResponseDto;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{userIdToFollow}")
    public ResponseEntity<SubscriptionResponseDto> followUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userIdToFollow) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.followUser(jwt, userIdToFollow));
    }

    @DeleteMapping("/{userIdToUnfollow}")
    public ResponseEntity<Void> unfollowUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userIdToUnfollow) {

        subscriptionService.unfollowUser(jwt, userIdToUnfollow);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check_user_follow/{userIdToCheck}")
    public ResponseEntity<FollowCheckResponseDto> checkUserFollow(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userIdToCheck) {

        return ResponseEntity.ok(subscriptionService.checkUserFollow(jwt, userIdToCheck));
    }

    @GetMapping("/followers/{id}")
    public ResponseEntity<Page<UserResponseDto>> getFollowers(
            @PathVariable UUID id,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(subscriptionService.getFollowers(id, pageable));
    }

    @GetMapping("/following/{id}")
    public ResponseEntity<Page<UserResponseDto>> getFollowing(
            @PathVariable UUID id,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(subscriptionService.getFollowing(id, pageable));
    }
}