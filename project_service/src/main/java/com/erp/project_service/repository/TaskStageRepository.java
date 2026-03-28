package com.erp.project_service.repository;

import com.erp.project_service.entity.TaskStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskStageRepository extends JpaRepository<TaskStage, Long> {
    List<TaskStage> findByProjectIdOrderByPosition(Long projectId);
    List<TaskStage> findByProjectIdIsNullOrderByPosition(); // global stages
    Optional<TaskStage> findByName(String name);
    Optional<TaskStage> findFirstByOrderByIdAsc();
}
