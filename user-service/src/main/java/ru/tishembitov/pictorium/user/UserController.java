package ru.tishembitov.pictorium.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.tishembitov.pictorium.file.FileStorageService;

import java.nio.file.Path;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        log.info("GET /me - Fetching current user with Keycloak ID: {}", jwt.getSubject());
        return ResponseEntity.ok(userService.getUserByKeycloakId(jwt.getSubject()));
    }

    @GetMapping("/user_id/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        log.info("GET /user_id/{} - Fetching user by ID", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/user_username/{username}")
    public ResponseEntity<UserResponseDto> getUserByUsername(@PathVariable String username) {
        log.info("GET /user_username/{} - Fetching user by username", username);
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PatchMapping("/information")
    public ResponseEntity<UserResponseDto> updateUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserUpdateDto userUpdateDto) {

        log.info("PATCH /information - Updating user with Keycloak ID: {}", jwt.getSubject());

        return ResponseEntity.ok(userService.updateUser(jwt.getSubject(), userUpdateDto));
    }

    @GetMapping("/upload/{id}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable UUID id) {
        log.info("GET /upload/{} - Fetching profile image", id);

        UserResponseDto user = userService.getUserById(id);
        if (user.image() == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = fileStorageService.getFile(user.image());

        try {
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error loading file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/banner/upload/{id}")
    public ResponseEntity<UserResponseDto> uploadBannerImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {

        log.info("POST /banner/upload/{} - Uploading banner image", id);
        return ResponseEntity.ok(userService.uploadBannerImage(id, file));
    }

    @GetMapping("/banner/upload/{id}")
    public ResponseEntity<Resource> getBannerImage(@PathVariable UUID id) {
        log.info("GET /banner/upload/{} - Fetching banner image", id);
        UserResponseDto user = userService.getUserById(id);

        if (user.bannerImage() == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = fileStorageService.getFile(user.bannerImage());

        try {
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error loading file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}