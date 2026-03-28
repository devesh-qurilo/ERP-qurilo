package com.erp.project_service.repository;

import com.erp.project_service.entity.ProjectUserState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectUserStateRepository extends JpaRepository<ProjectUserState, Long> {
    Optional<ProjectUserState> findByProjectIdAndUserId(Long projectId, String userId);
    List<ProjectUserState> findByUserIdAndPinnedAtNotNullOrderByPinnedAtDesc(String userId);
    List<ProjectUserState> findByUserIdAndArchivedAtNotNullOrderByArchivedAtDesc(String userId);

    List<ProjectUserState> findByUserIdAndProjectIdIn(String userId, Collection<Long> projectIds);

}
