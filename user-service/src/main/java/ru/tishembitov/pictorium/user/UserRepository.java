package ru.tishembitov.pictorium.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByKeycloakId(String keycloakId);

    @Query("SELECT u.id FROM User u WHERE u.keycloakId = :keycloakId")
    Optional<UUID> findIdByKeycloakId(String keycloakId);

    boolean existsByUsername(String username);

    boolean existsByKeycloakId(String keycloakId);
}