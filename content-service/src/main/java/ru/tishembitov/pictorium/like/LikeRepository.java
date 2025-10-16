package ru.tishembitov.pictorium.like;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    boolean existsByUserIdAndPinId(String userId, UUID pinId);

    @Query("SELECT l.pin.id FROM Like l WHERE l.userId = :userId AND l.pin.id IN :pinIds")
    Set<UUID> findPinIdsByUserIdAndPinIdIn(
            @Param("userId") String userId,
            @Param("pinIds") Set<UUID> pinIds
    );
}