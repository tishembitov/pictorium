package ru.tishembitov.pictorium.pin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PinInteractionRepository extends JpaRepository<Pin, UUID> {

    @Query("""
        SELECT new ru.tishembitov.pictorium.pin.PinInteractionProjection(
            p.id,
            CASE WHEN l.id IS NOT NULL THEN true ELSE false END,
            CASE WHEN s.id IS NOT NULL THEN true ELSE false END
        )
        FROM Pin p
        LEFT JOIN Like l ON l.pin.id = p.id AND l.userId = :userId
        LEFT JOIN SavedPin s ON s.pin.id = p.id AND s.userId = :userId
        WHERE p.id IN :pinIds
    """)
    List<PinInteractionProjection> findInteractions(
            @Param("userId") String userId,
            @Param("pinIds") Set<UUID> pinIds
    );
}
