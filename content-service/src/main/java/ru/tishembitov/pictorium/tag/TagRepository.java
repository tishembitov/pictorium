package ru.tishembitov.pictorium.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;


@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("""
        SELECT t FROM Tag t
        WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY t.name
    """)
    List<Tag> findByNameContainingIgnoreCase(@Param("query") String query, Pageable pageable);

    @Query("""
        SELECT DISTINCT t FROM Tag t 
        JOIN t.pins p 
        WHERE p.id = :pinId
        ORDER BY t.name
    """)
    List<Tag> findByPinId(@Param("pinId") UUID pinId);

    @Query("""
        SELECT DISTINCT t FROM Tag t 
        LEFT JOIN FETCH t.pins
        WHERE SIZE(t.pins) > 0
        ORDER BY t.name
    """)
    List<Tag> findAllWithPins();
}