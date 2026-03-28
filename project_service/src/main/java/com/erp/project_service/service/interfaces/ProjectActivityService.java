package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.activity.ProjectActivityDto;

import java.util.List;

public interface ProjectActivityService {
    void record(Long projectId, String actorEmployeeId, String action, String metadata);
    List<ProjectActivityDto> listForProject(Long projectId);
}
