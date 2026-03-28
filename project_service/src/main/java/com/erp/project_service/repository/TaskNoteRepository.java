package com.erp.project_service.repository;

import com.erp.project_service.entity.TaskNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskNoteRepository extends JpaRepository<TaskNote, Long> {
    List<TaskNote> findByTaskIdAndIsPublicTrue(Long taskId);
    List<TaskNote> findByTaskId(Long taskId);
}
