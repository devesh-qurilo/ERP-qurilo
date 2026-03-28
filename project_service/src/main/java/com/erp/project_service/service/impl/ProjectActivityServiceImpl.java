package com.erp.project_service.service.impl;

import com.erp.project_service.dto.activity.ProjectActivityDto;
import com.erp.project_service.entity.ProjectActivity;
import com.erp.project_service.mapper.ProjectActivityMapper;
import com.erp.project_service.repository.ProjectActivityRepository;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectActivityServiceImpl implements ProjectActivityService {

    private final ProjectActivityRepository repo;

    @Override
    public void record(Long projectId, String actorEmployeeId, String action, String metadata) {
        ProjectActivity a = ProjectActivity.builder()
                .projectId(projectId)
                .actorEmployeeId(actorEmployeeId)
                .action(action)
                .metadata(metadata)
                .build();
        repo.save(a);
    }

    @Override
    public List<ProjectActivityDto> listForProject(Long projectId) {
        return repo.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(ProjectActivityMapper::toDto).collect(Collectors.toList());
    }
}
