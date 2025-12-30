package ru.tishembitov.pictorium.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface BoardPinRepository extends JpaRepository<BoardPin, UUID> {

    boolean existsByBoardIdAndPinId(UUID boardId, UUID pinId);

    Optional<BoardPin> findByBoardIdAndPinId(UUID boardId, UUID pinId);

    @Query("SELECT bp.pin.id FROM BoardPin bp WHERE bp.board.userId = :userId AND bp.pin.id IN :pinIds")
    Set<UUID> findSavedPinIds(@Param("userId") String userId, @Param("pinIds") Set<UUID> pinIds);

    @Query("SELECT COUNT(bp) > 0 FROM BoardPin bp WHERE bp.board.userId = :userId AND bp.pin.id = :pinId")
    boolean isPinSavedByUser(@Param("userId") String userId, @Param("pinId") UUID pinId);

    @Query("""
    SELECT bp FROM BoardPin bp 
    WHERE bp.board.userId = :userId AND bp.pin.id = :pinId 
    ORDER BY bp.addedAt DESC 
    LIMIT 1
""")
    Optional<BoardPin> findLastSavedByUserAndPin(
            @Param("userId") String userId,
            @Param("pinId") UUID pinId
    );

    @Modifying
    @Query("DELETE FROM BoardPin bp WHERE bp.board.userId = :userId AND bp.pin.id = :pinId")
    int deleteByUserIdAndPinId(@Param("userId") String userId, @Param("pinId") UUID pinId);

    @Query("SELECT COUNT(DISTINCT bp.board.id) FROM BoardPin bp WHERE bp.board.userId = :userId AND bp.pin.id = :pinId")
    int countBoardsWithPin(@Param("userId") String userId, @Param("pinId") UUID pinId);
}