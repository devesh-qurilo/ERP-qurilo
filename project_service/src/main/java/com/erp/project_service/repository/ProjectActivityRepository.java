package com.erp.project_service.repository;

import com.erp.project_service.entity.ProjectActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectActivityRepository extends JpaRepository<ProjectActivity, Long> {
    List<ProjectActivity> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
