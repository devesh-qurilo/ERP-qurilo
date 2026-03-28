package com.erp.project_service.repository;

import com.erp.project_service.entity.FileMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileMetaRepository extends JpaRepository<FileMeta, Long> {
    List<FileMeta> findByProjectId(Long projectId);
    List<FileMeta> findByTaskId(Long taskId);
    List<FileMeta> findByMilestoneId(Long milestoneId);
    // if you added recurring_task_id
    List<FileMeta> findByRecurringTaskId(Long recurringTaskId);
}
