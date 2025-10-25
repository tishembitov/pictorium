package ru.tishembitov.pictorium.pin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PinRepository extends JpaRepository<Pin, UUID>, JpaSpecificationExecutor<Pin> {

    @Query("SELECT DISTINCT p FROM Pin p LEFT JOIN FETCH p.tags WHERE p.id IN :ids")
    List<Pin> findAllByIdWithTags(@Param("ids") List<UUID> ids);

    @Query("SELECT DISTINCT p FROM Pin p LEFT JOIN FETCH p.tags WHERE p.id = :id")
    Optional<Pin> findByIdWithTags(@Param("id") UUID id);

    Optional<Pin> findTopByOrderByCreatedAtDesc();

    @Query("""
    SELECT p.id as id,
           CASE WHEN l.id IS NOT NULL THEN true ELSE false END as liked,
           CASE WHEN s.id IS NOT NULL THEN true ELSE false END as saved
    FROM Pin p
    LEFT JOIN Like l ON l.pin.id = p.id AND l.userId = :userId
    LEFT JOIN SavedPin s ON s.pin.id = p.id AND s.userId = :userId
    WHERE p.id IN :pinIds
""")
    List<PinInteractionProjection> findUserInteractions(
            @Param("userId") String userId,
            @Param("pinIds") Set<UUID> pinIds
    );

    @Modifying
    @Query("UPDATE Pin p SET p.likeCount = p.likeCount + 1 WHERE p.id = :pinId")
    void incrementLikeCount(@Param("pinId") UUID pinId);

    @Modifying
    @Query("UPDATE Pin p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :pinId")
    void decrementLikeCount(@Param("pinId") UUID pinId);

    @Modifying
    @Query("UPDATE Pin p SET p.saveCount = p.saveCount + 1 WHERE p.id = :pinId")
    void incrementSaveCount(@Param("pinId") UUID pinId);

    @Modifying
    @Query("UPDATE Pin p SET p.saveCount = CASE WHEN p.saveCount > 0 THEN p.saveCount - 1 ELSE 0 END WHERE p.id = :pinId")
    void decrementSaveCount(@Param("pinId") UUID pinId);

    @Modifying
    @Query("UPDATE Pin p SET p.commentCount = p.commentCount + 1 WHERE p.id = :pinId")
    void incrementCommentCount(@Param("pinId") UUID pinId);

    @Modifying
    @Query("UPDATE Pin p SET p.commentCount = CASE WHEN p.commentCount > 0 THEN p.commentCount - 1 ELSE 0 END WHERE p.id = :pinId")
    void decrementCommentCount(@Param("pinId") UUID pinId);

    @Modifying
    @Query("UPDATE Pin p SET p.commentCount = CASE WHEN p.commentCount >= :count THEN p.commentCount - :count ELSE 0 END WHERE p.id = :pinId")
    void decrementCommentCountBy(@Param("pinId") UUID pinId, @Param("count") long count);

    @Query("SELECT p FROM Board b JOIN b.pins p WHERE b.id = :boardId")
    Page<Pin> findByBoardId(@Param("boardId") UUID boardId, Pageable pageable);
}