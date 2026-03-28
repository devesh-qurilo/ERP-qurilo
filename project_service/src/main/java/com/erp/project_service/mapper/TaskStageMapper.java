package com.erp.project_service.mapper;

import com.erp.project_service.dto.task.TaskStageDto;
import com.erp.project_service.entity.TaskStage;

public final class TaskStageMapper {
    private TaskStageMapper() {}

    public static TaskStageDto toDto(TaskStage e) {
        if (e == null) return null;
        TaskStageDto d = new TaskStageDto();
        d.setId(e.getId());
        d.setName(e.getName());
        d.setPosition(e.getPosition());
        d.setLabelColor(e.getLabelColor());
        d.setProjectId(e.getProjectId());
        d.setCreatedBy(e.getCreatedBy());
        return d;
    }
}
