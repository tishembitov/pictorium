package ru.tishembitov.pictorium.like;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    boolean existsByUserIdAndPinId(String userId, UUID pinId);
    Optional<Like> findByUserIdAndPinId(String userId, UUID pinId);
    Page<Like> findByPinIdOrderByCreatedAtDesc(UUID pinId, Pageable pageable);

    @Query("SELECT l.pin.id FROM Like l WHERE l.userId = :userId AND l.pin.id IN :pinIds")
    Set<UUID> findLikedPinIds(@Param("userId") String userId, @Param("pinIds") Set<UUID> pinIds);

    boolean existsByUserIdAndCommentId(String userId, UUID commentId);
    Optional<Like> findByUserIdAndCommentId(String userId, UUID commentId);
    Page<Like> findByCommentIdOrderByCreatedAtDesc(UUID commentId, Pageable pageable);
}
