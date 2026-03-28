package com.erp.project_service.repository;

import com.erp.project_service.entity.TaskCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskCategoryRepository extends JpaRepository<TaskCategory, Long> {
    Optional<TaskCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
