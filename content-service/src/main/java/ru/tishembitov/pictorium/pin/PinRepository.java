package ru.tishembitov.pictorium.pin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PinRepository extends JpaRepository<Pin, UUID>, JpaSpecificationExecutor<Pin> {

    @Query("SELECT DISTINCT p FROM Pin p LEFT JOIN FETCH p.tags WHERE p.id IN :ids")
    List<Pin> findAllByIdWithTags(@Param("ids") List<UUID> ids);

    @Query("SELECT DISTINCT p FROM Pin p LEFT JOIN FETCH p.tags WHERE p.id = :id")
    Optional<Pin> findByIdWithTags(@Param("id") UUID id);
}