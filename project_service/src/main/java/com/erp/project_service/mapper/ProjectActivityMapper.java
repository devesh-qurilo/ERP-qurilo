package com.erp.project_service.mapper;

import com.erp.project_service.dto.activity.ProjectActivityDto;
import com.erp.project_service.entity.ProjectActivity;

public final class ProjectActivityMapper {
    private ProjectActivityMapper() {}

    public static ProjectActivityDto toDto(ProjectActivity e) {
        if (e == null) return null;
        ProjectActivityDto d = new ProjectActivityDto();
        d.setId(e.getId());
        d.setProjectId(e.getProjectId());
        d.setActorEmployeeId(e.getActorEmployeeId());
        d.setAction(e.getAction());
        d.setMetadata(e.getMetadata());
        d.setCreatedAt(e.getCreatedAt());
        return d;
    }
}
