package ru.tishembitov.pictorium.pin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PinRepository extends JpaRepository<Pin, UUID>, JpaSpecificationExecutor<Pin> {
}