package ru.tishembitov.pictorium.selectedBoard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SelectedBoardRepository extends JpaRepository<SelectedBoard, UUID> {

    Optional<SelectedBoard> findByUserId(String userId);

    @Query("SELECT sb FROM SelectedBoard sb JOIN FETCH sb.selectedBoard WHERE sb.userId = :userId")
    Optional<SelectedBoard> findByUserIdWithBoard(@Param("userId") String userId);
}
