package ru.tishembitov.pictorium.savedPins;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface SavedPinRepository extends JpaRepository<SavedPin, UUID> {

    boolean existsByUserIdAndPinId(String userId, UUID pinId);

    @Query("SELECT sp.pin.id FROM SavedPin sp WHERE sp.userId = :userId AND sp.pin.id IN :pinIds")
    Set<UUID> findPinIdsByUserIdAndPinIdIn(
            @Param("userId") String userId,
            @Param("pinIds") Set<UUID> pinIds
    );
}