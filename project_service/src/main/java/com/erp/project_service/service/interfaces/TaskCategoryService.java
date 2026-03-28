package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.task.TaskCategoryDto;

import java.util.List;

public interface TaskCategoryService {
    TaskCategoryDto create(String name, String createdBy);
    List<TaskCategoryDto> listAll();
    void delete(Long id, String actor);
}
