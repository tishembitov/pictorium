package ru.tishembitov.pictorium.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardRepository extends JpaRepository<Board, UUID> {

    List<Board> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Board> findByIdAndUserId(UUID id, String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Board b SET b.pinCount = b.pinCount + 1 WHERE b.id = :boardId")
    void incrementPinCount(@Param("boardId") UUID boardId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Board b SET b.pinCount = b.pinCount - 1 WHERE b.id = :boardId AND b.pinCount > 0")
    void decrementPinCount(@Param("boardId") UUID boardId);

    boolean existsByUserIdAndTitle(String userId, String title);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Board b JOIN b.pins p " +
            "WHERE b.id = :boardId AND p.id = :pinId")
    boolean existsPinInBoard(@Param("boardId") UUID boardId, @Param("pinId") UUID pinId);

    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.pins WHERE b.id = :boardId")
    Optional<Board> findByIdWithPins(@Param("boardId") UUID boardId);

    @Query("SELECT (COUNT(p) > 0) FROM Board b JOIN b.pins p WHERE b.id = :boardId AND p.id = :pinId")
    boolean existsPinOnBoard(UUID boardId, UUID pinId);
}