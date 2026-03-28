package com.erp.project_service.repository;

import com.erp.project_service.entity.ProjectNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectNoteRepository extends JpaRepository<ProjectNote, Long> {
    List<ProjectNote> findByProjectIdAndIsPublicTrue(Long projectId);
    List<ProjectNote> findByProjectId(Long projectId);
}
