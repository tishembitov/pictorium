package ru.tishembitov.pictorium.savedPin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.tishembitov.pictorium.pin.Pin;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SavedPinRepository extends JpaRepository<SavedPin, UUID> {

    boolean existsByUserIdAndPinId(String userId, UUID pinId);

    Optional<SavedPin> findByUserIdAndPinId(String userId, UUID pinId);

    void deleteByUserIdAndPinId(String userId, UUID pinId);

    @Query("""
        SELECT sp.pin FROM SavedPin sp 
        WHERE sp.userId = :userId 
        ORDER BY sp.createdAt DESC
    """)
    Page<Pin> findSavedPinsByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("""
        SELECT sp.pin.id FROM SavedPin sp 
        WHERE sp.userId = :userId AND sp.pin.id IN :pinIds
    """)
    Set<UUID> findSavedToProfilePinIds(
            @Param("userId") String userId,
            @Param("pinIds") Set<UUID> pinIds
    );

    long countByUserId(String userId);

    boolean existsByPinId(UUID pinId);

    @Query("SELECT COUNT(DISTINCT sp.userId) FROM SavedPin sp WHERE sp.pin.id = :pinId")
    long countUniqueSaversForPin(@Param("pinId") UUID pinId);
}