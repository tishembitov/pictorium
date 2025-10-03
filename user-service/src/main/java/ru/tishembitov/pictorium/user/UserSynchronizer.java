package ru.tishembitov.pictorium.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSynchronizer {

    private final UserRepository userRepository;

    public User createUserFromToken(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String username = generateUniqueUsername(email);
        String keycloakId = jwt.getSubject();

        log.info("Creating new user from token: keycloakId={}, email={}", keycloakId, email);

        User newUser = User.builder()
                .keycloakId(keycloakId)
                .email(email)
                .username(username)
                .createdAt(Instant.now())
                .build();

        return userRepository.save(newUser);
    }


    private String generateUniqueUsername(String email) {
        String username = email.substring(0, email.indexOf('@'));

        if (username.contains("+")) {
            username = username.substring(0, username.indexOf('+'));
        }

        username = username.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username += counter;
            counter++;
        }

        return username;
    }
}
