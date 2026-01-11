package ru.tishembitov.pictorium.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {


    @Query("""
        SELECT c FROM Chat c 
        WHERE c.senderId = :userId OR c.recipientId = :userId 
        ORDER BY c.updatedAt DESC
    """)
    List<Chat> findAllByUserId(@Param("userId") String userId);

    @Query("""
        SELECT c FROM Chat c 
        WHERE (c.senderId = :userId1 AND c.recipientId = :userId2) 
           OR (c.senderId = :userId2 AND c.recipientId = :userId1)
    """)
    Optional<Chat> findByParticipants(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2
    );


    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Chat c 
        WHERE (c.senderId = :userId1 AND c.recipientId = :userId2) 
           OR (c.senderId = :userId2 AND c.recipientId = :userId1)
    """)
    boolean existsByParticipants(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2
    );
}