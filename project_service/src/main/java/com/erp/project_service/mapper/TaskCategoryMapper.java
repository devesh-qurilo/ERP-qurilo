package com.erp.project_service.mapper;

import com.erp.project_service.dto.task.TaskCategoryDto;
import com.erp.project_service.entity.TaskCategory;

public final class TaskCategoryMapper {
    private TaskCategoryMapper() {}

    public static TaskCategoryDto toDto(TaskCategory e) {
        if (e == null) return null;
        TaskCategoryDto d = new TaskCategoryDto();
        d.setId(e.getId());
        d.setName(e.getName());
        d.setCreatedBy(e.getCreatedBy());
        return d;
    }
}
