package ru.tishembitov.pictorium.savedPin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedPinRepository extends JpaRepository<SavedPin, UUID> {

    boolean existsByUserIdAndPinId(String userId, UUID pinId);
    Optional<SavedPin> findByUserIdAndPinId(String userId, UUID pinId);
}