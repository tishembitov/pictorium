package ru.tishembitov.pictorium.like;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    boolean existsByUserIdAndPinId(String userId, UUID pinId);
    Optional<Like> findByUserIdAndPinId(String userId, UUID pinId);
}