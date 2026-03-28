package com.erp.project_service.service;

import com.erp.project_service.entity.Project;
import com.erp.project_service.entity.ProjectUserState;
import com.erp.project_service.repository.ProjectUserStateRepository;
import com.erp.project_service.service.interfaces.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectUserStateService {

    private final ProjectUserStateRepository repo;
    private final ProjectService projectService; // reuse for visibility/auth check

    // bas ye helper check karega ki actor ko project dekhne ka right hai ya nahi
    private void ensureProjectVisibleToActor(Long projectId, String actor) {
        // yeh call authorization/visibility enforce karega (Admin ko sab allowed, Employee ko sirf assigned)
        projectService.getProject(projectId, actor);
    }

    private ProjectUserState getOrCreate(Long projectId, String userId) {
        return repo.findByProjectIdAndUserId(projectId, userId)
                .orElse(ProjectUserState.builder()
                        .projectId(projectId).userId(userId).build());
    }

    public void pin(Long projectId, String actor) {
        ensureProjectVisibleToActor(projectId, actor);
        ProjectUserState s = getOrCreate(projectId, actor);
        if (s.getPinnedAt() == null) s.setPinnedAt(Instant.now());
        repo.save(s);
    }

    public void unpin(Long projectId, String actor) {
        ensureProjectVisibleToActor(projectId, actor);
        ProjectUserState s = getOrCreate(projectId, actor);
        if (s.getPinnedAt() != null) s.setPinnedAt(null);
        repo.save(s);
    }

    public void archive(Long projectId, String actor) {
        ensureProjectVisibleToActor(projectId, actor);
        ProjectUserState s = getOrCreate(projectId, actor);
        if (s.getArchivedAt() == null) s.setArchivedAt(Instant.now());
        repo.save(s);
    }

    public void unarchive(Long projectId, String actor) {
        ensureProjectVisibleToActor(projectId, actor);
        ProjectUserState s = getOrCreate(projectId, actor);
        if (s.getArchivedAt() != null) s.setArchivedAt(null);
        repo.save(s);
    }

    public List<ProjectUserState> listPinned(String actor) {
        return repo.findByUserIdAndPinnedAtNotNullOrderByPinnedAtDesc(actor);
    }

    public List<ProjectUserState> listArchived(String actor) {
        return repo.findByUserIdAndArchivedAtNotNullOrderByArchivedAtDesc(actor);
    }
}
