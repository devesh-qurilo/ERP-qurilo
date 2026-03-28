package com.erp.project_service.service.impl;

import com.erp.project_service.dto.task.TaskCategoryDto;
import com.erp.project_service.entity.TaskCategory;
import com.erp.project_service.exception.ResourceNotFoundException;
import com.erp.project_service.mapper.TaskCategoryMapper;
import com.erp.project_service.repository.TaskCategoryRepository;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import com.erp.project_service.service.interfaces.TaskCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskCategoryServiceImpl implements TaskCategoryService {

    private final TaskCategoryRepository repo;
    private final ProjectActivityService activityService;

    @Override
    @Transactional
    public TaskCategoryDto create(String name, String createdBy) {
        TaskCategory c = TaskCategory.builder().name(name).createdBy(createdBy).build();
        TaskCategory saved = repo.save(c);
        // no project id for category; record as global activity (projectId null)
        activityService.record(null, createdBy, "TASK_CATEGORY_CREATED", name);
        return TaskCategoryMapper.toDto(saved);
    }

    @Override
    public List<TaskCategoryDto> listAll() {
        return repo.findAll().stream().map(TaskCategoryMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id, String actor) {
        TaskCategory t = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaskCategory", "id", id.toString()));
        repo.deleteById(id);
        activityService.record(null, actor, "TASK_CATEGORY_DELETED", String.valueOf(id));
    }
}
