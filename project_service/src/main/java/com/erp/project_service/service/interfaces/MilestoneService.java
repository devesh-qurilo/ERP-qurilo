package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.milestone.MilestoneCreateRequest;
import com.erp.project_service.dto.milestone.MilestoneDto;

import java.util.List;

public interface MilestoneService {
    MilestoneDto create(Long projectId, MilestoneCreateRequest req, String createdBy);

    List<MilestoneDto> listByProject(Long projectId, String requesterId);

    MilestoneDto get(Long projectId, Long milestoneId);
    MilestoneDto update(Long projectId, Long milestoneId, MilestoneCreateRequest req, String updatedBy);
    void delete(Long projectId, Long milestoneId, String deletedBy);
    MilestoneDto changeStatus(Long projectId, Long milestoneId, String newStatus, String actor);

    List<MilestoneDto> listByProjects(Long projectId);
}
