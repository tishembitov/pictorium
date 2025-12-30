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

    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.boardPins WHERE b.id = :boardId")
    Optional<Board> findByIdWithPins(@Param("boardId") UUID boardId);

    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.boardPins WHERE b.id = :boardId AND b.userId = :userId")
    Optional<Board> findByIdWithPinsAndUserId(@Param("boardId") UUID boardId, @Param("userId") String userId);

    @Query("""
        SELECT b.id as id, 
               b.userId as userId,
               b.title as title,
               b.createdAt as createdAt,
               b.updatedAt as updatedAt,
               CASE WHEN bp.id IS NOT NULL THEN true ELSE false END as hasPin,
               (SELECT COUNT(bp2) FROM BoardPin bp2 WHERE bp2.board = b) as pinCount
        FROM Board b 
        LEFT JOIN b.boardPins bp ON bp.pin.id = :pinId
        WHERE b.userId = :userId
        ORDER BY b.createdAt DESC
    """)
    List<BoardWithPinStatusProjection> findUserBoardsWithPinStatus(
            @Param("userId") String userId,
            @Param("pinId") UUID pinId
    );

    @Query("""
        SELECT bp.pin.id as pinId,
               (SELECT b2.title FROM BoardPin bp2 
                JOIN bp2.board b2 
                WHERE b2.userId = :userId AND bp2.pin.id = bp.pin.id 
                ORDER BY bp2.addedAt DESC 
                LIMIT 1) as lastBoardName,
               COUNT(DISTINCT bp.board.id) as boardCount
        FROM BoardPin bp 
        WHERE bp.board.userId = :userId AND bp.pin.id IN :pinIds
        GROUP BY bp.pin.id
    """)
    List<PinSaveInfoProjection> findPinSaveInfo(
            @Param("userId") String userId,
            @Param("pinIds") Set<UUID> pinIds
    );

    @Query("""
        SELECT bp.pin.id FROM BoardPin bp 
        WHERE bp.pin.id IN :pinIds
        AND bp.board.userId = :userId
        GROUP BY bp.pin.id
        HAVING COUNT(DISTINCT bp.board.id) = 1
        AND MAX(CASE WHEN bp.board.id = :excludeBoardId THEN 1 ELSE 0 END) = 1
    """)
    Set<UUID> findPinsOnlyInBoard(
            @Param("userId") String userId,
            @Param("pinIds") Set<UUID> pinIds,
            @Param("excludeBoardId") UUID excludeBoardId
    );

    @Query("SELECT SIZE(b.boardPins) FROM Board b WHERE b.id = :boardId")
    int countPinsInBoard(@Param("boardId") UUID boardId);
}