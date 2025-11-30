package ru.tishembitov.pictorium.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSynchronizer {

    private final UserRepository userRepository;

    @Transactional
    public void createUserFromToken(Jwt jwt) {
        String id = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String username = extractUsername(jwt, email);

        log.info("Creating new user from token: id={}, email={}, username={}",
                id, email, username);

        User newUser = User.builder()
                .id(id)
                .email(email)
                .username(username)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        userRepository.save(newUser);
    }

    /**
     * Извлекает username из JWT токена Keycloak
     */
    private String extractUsername(Jwt jwt, String email) {
        // Keycloak хранит username в claim "preferred_username"
        String username = jwt.getClaimAsString("preferred_username");

        if (username == null || username.isBlank()) {
            username = jwt.getClaimAsString("username");
        }

        if (username == null || username.isBlank()) {
            username = jwt.getClaimAsString("name");
        }

        if (username == null || username.isBlank()) {
            if (email != null && email.contains("@")) {
                username = email.split("@")[0] + "_" +
                        (System.currentTimeMillis() % 100000);
            } else {
                username = "user_" + java.util.UUID.randomUUID()
                        .toString().substring(0, 8);
            }
            log.warn("Username not found in token, generated: {}", username);
        }

        return username;
    }
}