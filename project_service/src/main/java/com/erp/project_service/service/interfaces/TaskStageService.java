package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.task.TaskStageDto;

import java.util.List;

public interface TaskStageService {
    TaskStageDto create(TaskStageDto dto, String createdBy);
    TaskStageDto update(Long id, TaskStageDto dto, String updatedBy);
    void delete(Long id, String deletedBy);
    List<TaskStageDto> listForProject(Long projectId);

    List<TaskStageDto> getAll();
}
