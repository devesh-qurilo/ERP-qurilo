package com.erp.lead_service.repository;

import com.erp.lead_service.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StageRepository extends JpaRepository<Stage, Long> {
    Optional<Stage> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
