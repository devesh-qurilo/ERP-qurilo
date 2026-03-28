package com.erp.project_service.service.impl;

import com.erp.project_service.dto.task.TaskStageDto;
import com.erp.project_service.entity.Task;
import com.erp.project_service.entity.TaskStage;
import com.erp.project_service.exception.BadRequestException;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.mapper.TaskStageMapper;
import com.erp.project_service.repository.TaskRepository;
import com.erp.project_service.repository.TaskStageRepository;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import com.erp.project_service.service.interfaces.TaskStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskStageServiceImpl implements TaskStageService {

    private final TaskStageRepository repo;
    private final ProjectActivityService activityService;
    private final TaskRepository taskRepository; // added


    @Override
    @Transactional
    public TaskStageDto create(TaskStageDto dto, String createdBy) {
        TaskStage e = TaskStage.builder()
                .name(dto.getName())
                .position(dto.getPosition())
                .labelColor(dto.getLabelColor())
                .projectId(dto.getProjectId())
                .createdBy(createdBy)
                .build();
        TaskStage saved = repo.save(e);
        activityService.record(dto.getProjectId(), createdBy, "TASKSTAGE_CREATED", String.valueOf(saved.getId()));
        return TaskStageMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TaskStageDto update(Long id, TaskStageDto dto, String updatedBy) {
        TaskStage e = repo.findById(id).orElseThrow(() -> new NotFoundException("TaskStage not found"));
        e.setName(dto.getName() != null ? dto.getName() : e.getName());
        e.setLabelColor(dto.getLabelColor() != null ? dto.getLabelColor() : e.getLabelColor());
        e.setPosition(dto.getPosition() != null ? dto.getPosition() : e.getPosition());
        e.setUpdatedBy(updatedBy);
        TaskStage saved = repo.save(e);
        activityService.record(e.getProjectId(), updatedBy, "TASKSTAGE_UPDATED", String.valueOf(saved.getId()));
        return TaskStageMapper.toDto(saved);
    }

//    @Override
//    @Transactional
//    public void delete(Long id, String deletedBy) {
//        TaskStage e = repo.findById(id).orElseThrow(() -> new NotFoundException("TaskStage not found"));
//        repo.deleteById(id);
//        activityService.record(e.getProjectId(), deletedBy, "TASKSTAGE_DELETED", String.valueOf(id));
//    }

//    @Override
//    @Transactional
//    public void delete(Long id, String deletedBy) {
//        TaskStage stage = repo.findById(id).orElseThrow(() -> new NotFoundException("TaskStage not found"));
//
//        // find tasks using this stage
//        List<Task> tasksUsingStage = taskRepository.findByTaskStageId(id);
//
//        if (!tasksUsingStage.isEmpty()) {
//            // try to fetch a default fallback stage
//            TaskStage fallback = repo.findFirstByOrderByIdAsc().orElse(null);
//
//            // if you want a specially-named fallback, try:
//            // TaskStage fallback = repo.findByName("INCOMPLETE").orElse(null);
//
//            if (fallback == null || Objects.equals(fallback.getId(), id)) {
//                // No safe fallback found — refuse to delete
//                throw new BadRequestException("Cannot delete task stage: there are " + tasksUsingStage.size() +
//                        " tasks referencing it and no fallback stage exists. Create a fallback stage first.");
//            }
//
//            // Reassign tasks to fallback stage
//            for (Task t : tasksUsingStage) {
//                t.setTaskStage(fallback);
//            }
//            taskRepository.saveAll(tasksUsingStage);
//        }
//
//        // safe to delete now
//        repo.deleteById(id);
//
//        activityService.record(stage.getProjectId(), deletedBy, "TASKSTAGE_DELETED", String.valueOf(id));
//    }

    @Override
    @Transactional
    public void delete(Long id, String deletedBy) {
        TaskStage stage = repo.findById(id).orElseThrow(() -> new NotFoundException("TaskStage not found"));

        // find tasks using this stage
        List<Task> tasksUsingStage = taskRepository.findByTaskStageId(id);

        if (!tasksUsingStage.isEmpty()) {
            // Set task_stage to null for all referencing tasks
            for (Task t : tasksUsingStage) {
                t.setTaskStage(null);
            }
            taskRepository.saveAll(tasksUsingStage);
        }

        // safe to delete now
        repo.deleteById(id);

        activityService.record(stage.getProjectId(), deletedBy, "TASKSTAGE_DELETED", String.valueOf(id));
    }


    @Override
    public List<TaskStageDto> listForProject(Long projectId) {
        return repo.findByProjectIdOrderByPosition(projectId).stream().map(TaskStageMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<TaskStageDto> getAll(){
        return repo.findAll().stream().map(TaskStageMapper::toDto).collect(Collectors.toList());
    }
}
