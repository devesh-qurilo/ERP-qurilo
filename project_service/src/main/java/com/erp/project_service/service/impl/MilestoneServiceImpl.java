package com.erp.project_service.service.impl;

import com.erp.project_service.dto.milestone.MilestoneCreateRequest;
import com.erp.project_service.dto.milestone.MilestoneDto;
import com.erp.project_service.entity.Project;
import com.erp.project_service.entity.ProjectMilestone;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.mapper.MilestoneMapper;
import com.erp.project_service.repository.ProjectMilestoneRepository;
import com.erp.project_service.repository.ProjectRepository;
import com.erp.project_service.security.SecurityUtils;
import com.erp.project_service.service.interfaces.MilestoneService;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilestoneServiceImpl implements MilestoneService {

    private final ProjectMilestoneRepository repo;
    private final ProjectActivityService activityService;
    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public MilestoneDto create(Long projectId, MilestoneCreateRequest req, String createdBy) {
        ProjectMilestone m = MilestoneMapper.toEntity(req);
        m.setProjectId(projectId);
        m.setCreatedBy(createdBy);
        ProjectMilestone saved = repo.save(m);
        activityService.record(projectId, createdBy, "MILESTONE_CREATED", String.valueOf(saved.getId()));
        return MilestoneMapper.toDto(saved);
    }

    @Override
    public List<MilestoneDto> listByProject(Long projectId, String requesterId) {
        // ✅ First check if project exists
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        // ✅ Check if user has access to this project
        checkProjectAccess(project, requesterId);

        // ✅ Now fetch milestones
        return repo.findByProjectId(projectId).stream()
                .map(MilestoneMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public MilestoneDto get(Long projectId, Long milestoneId) {
        ProjectMilestone m = repo.findById(milestoneId).orElseThrow(() -> new NotFoundException("Milestone not found"));
        if (!m.getProjectId().equals(projectId)) throw new NotFoundException("Milestone not found in project");
        return MilestoneMapper.toDto(m);
    }

    @Override
    @Transactional
    public MilestoneDto update(Long projectId, Long milestoneId, MilestoneCreateRequest req, String updatedBy) {
        ProjectMilestone m = repo.findById(milestoneId).orElseThrow(() -> new NotFoundException("Milestone not found"));
        if (!m.getProjectId().equals(projectId)) throw new NotFoundException("Milestone not found in project");
        // basic apply
        m.setTitle(req.getTitle() != null ? req.getTitle() : m.getTitle());
        m.setMilestoneCost(req.getMilestoneCost() != null ? req.getMilestoneCost() : m.getMilestoneCost());
        if (req.getStatus() != null) m.setStatus(com.erp.project_service.entity.MilestoneStatus.valueOf(req.getStatus()));
        m.setSummary(req.getSummary() != null ? req.getSummary() : m.getSummary());
        m.setStartDate(req.getStartDate() != null ? req.getStartDate() : m.getStartDate());
        m.setEndDate(req.getEndDate() != null ? req.getEndDate() : m.getEndDate());
        m.setUpdatedBy(updatedBy);
        ProjectMilestone saved = repo.save(m);
        activityService.record(projectId, updatedBy, "MILESTONE_UPDATED", String.valueOf(saved.getId()));
        return MilestoneMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long projectId, Long milestoneId, String deletedBy) {
        ProjectMilestone m = repo.findById(milestoneId).orElseThrow(() -> new NotFoundException("Milestone not found"));
        if (!m.getProjectId().equals(projectId)) throw new NotFoundException("Milestone not found in project");
        repo.deleteById(milestoneId);
        activityService.record(projectId, deletedBy, "MILESTONE_DELETED", milestoneId.toString());
    }

    @Override
    @Transactional
    public MilestoneDto changeStatus(Long projectId, Long milestoneId, String newStatus, String actor) {
        ProjectMilestone m = repo.findById(milestoneId).orElseThrow(() -> new NotFoundException("Milestone not found"));
        if (!m.getProjectId().equals(projectId)) throw new NotFoundException("Milestone not found in project");
        m.setStatus(com.erp.project_service.entity.MilestoneStatus.valueOf(newStatus));
        m.setUpdatedBy(actor);
        ProjectMilestone saved = repo.save(m);
        activityService.record(projectId, actor, "MILESTONE_STATUS_CHANGED", newStatus);
        return MilestoneMapper.toDto(saved);
    }

    @Override
    public List<MilestoneDto> listByProjects(Long projectId) {
        return repo.findByProjectId(projectId).stream().map(MilestoneMapper::toDto).collect(Collectors.toList());
    }

    // ✅ RBAC Check Method
    private void checkProjectAccess(Project project, String userId) {
        boolean isAdmin = SecurityUtils.isAdmin();
        boolean isAssigned = project.getAssignedEmployeeIds().contains(userId);
        boolean isProjectOwner = userId.equals(project.getCreatedBy());

        if (!isAdmin && !isAssigned && !isProjectOwner) {
            throw new AccessDeniedException("Access denied: You are not assigned to this project");
        }
    }
}
