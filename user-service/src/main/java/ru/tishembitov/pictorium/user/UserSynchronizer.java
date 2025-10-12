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
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("username");
        String id = jwt.getSubject();

        log.info("Creating new user from token: id={}", id);

        User newUser = User.builder()
                .id(id)
                .email(email)
                .username(username)
                .createdAt(Instant.now())
                .build();

        userRepository.save(newUser);
    }
}
