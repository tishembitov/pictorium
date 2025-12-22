package ru.tishembitov.pictorium.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface BoardRepository extends JpaRepository<Board, UUID> {

    List<Board> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Board> findByIdAndUserId(UUID id, String userId);

    boolean existsByUserIdAndTitle(String userId, String title);

    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.pins WHERE b.id = :boardId")
    Optional<Board> findByIdWithPins(@Param("boardId") UUID boardId);

    @Query("SELECT COUNT(p) > 0 FROM Board b JOIN b.pins p WHERE b.id = :boardId AND p.id = :pinId")
    boolean existsPinInBoard(@Param("boardId") UUID boardId, @Param("pinId") UUID pinId);

    @Query("SELECT COUNT(b) > 0 FROM Board b JOIN b.pins p WHERE b.userId = :userId AND p.id = :pinId")
    boolean isPinSavedByUser(@Param("userId") String userId, @Param("pinId") UUID pinId);

    @Query("SELECT DISTINCT p.id FROM Board b JOIN b.pins p WHERE b.userId = :userId AND p.id IN :pinIds")
    Set<UUID> findSavedPinIds(@Param("userId") String userId, @Param("pinIds") Set<UUID> pinIds);

    @Query("""
        SELECT b.id as id, 
               b.userId as userId,
               b.title as title,
               b.createdAt as createdAt,
               b.updatedAt as updatedAt,
               CASE WHEN p.id IS NOT NULL THEN true ELSE false END as hasPin,
               SIZE(b.pins) as pinCount
        FROM Board b 
        LEFT JOIN b.pins p ON p.id = :pinId
        WHERE b.userId = :userId
        ORDER BY b.createdAt DESC
    """)
    List<BoardWithPinStatusProjection> findUserBoardsWithPinStatus(
            @Param("userId") String userId,
            @Param("pinId") UUID pinId
    );

    @Query("""
    SELECT p.id as pinId, 
           MIN(b.title) as firstBoardName,
           COUNT(b.id) as boardCount
    FROM Board b 
    JOIN b.pins p 
    WHERE b.userId = :userId AND p.id IN :pinIds
    GROUP BY p.id
""")
    List<PinSaveInfoProjection> findPinSaveInfo(
            @Param("userId") String userId,
            @Param("pinIds") Set<UUID> pinIds
    );

    @Query("SELECT SIZE(b.pins) FROM Board b WHERE b.id = :boardId")
    int countPinsInBoard(@Param("boardId") UUID boardId);

    @Query("SELECT b FROM Board b JOIN b.pins p WHERE b.userId = :userId AND p.id = :pinId")
    List<Board> findBoardsContainingPin(@Param("userId") String userId, @Param("pinId") UUID pinId);

    @Query("SELECT COUNT(DISTINCT b.userId) FROM Board b JOIN b.pins p WHERE p.id = :pinId")
    long countUniqueSaversForPin(@Param("pinId") UUID pinId);
}