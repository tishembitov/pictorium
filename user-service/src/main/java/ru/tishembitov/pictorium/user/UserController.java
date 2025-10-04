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
        return ResponseEntity.ok(userService.getCurrentUser(jwt));
    }

    @GetMapping("/user_id/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/user_username/{username}")
    public ResponseEntity<UserResponseDto> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PatchMapping(value = "/information")
    public ResponseEntity<UserResponseDto> updateUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserUpdateDto userUpdateDto) {

        return ResponseEntity.ok(userService.updateUser(jwt, userUpdateDto));
    }

    @PostMapping("/upload")
    public ResponseEntity<UserResponseDto> uploadProfileImage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(userService.uploadProfileImage(jwt, file));
    }

    @GetMapping("/upload/{id}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable UUID id) {

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

    @PostMapping("/banner/upload")
    public ResponseEntity<UserResponseDto> uploadBannerImage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(userService.uploadBannerImage(jwt, file));
    }

    @GetMapping("/banner/upload/{id}")
    public ResponseEntity<Resource> getBannerImage(@PathVariable UUID id) {
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