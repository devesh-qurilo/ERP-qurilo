package com.erp.project_service.mapper;

import com.erp.project_service.dto.task.EmployeeTaskCreateRequest;
import com.erp.project_service.dto.task.TaskCreateRequest;
import com.erp.project_service.dto.task.TaskDto;
import com.erp.project_service.entity.Task;
import com.erp.project_service.entity.TaskPriority;

public final class TaskMapper {
    private TaskMapper() {}

    public static Task toEntity(TaskCreateRequest r) {
        if (r == null) return null;
        Task t = Task.builder()
                .title(r.getTitle())
                .categoryId(r.getCategoryId())
                .projectId(r.getProjectId())
                .startDate(r.getStartDate())
                .dueDate(r.getDueDate())
                .noDueDate(r.getNoDueDate() != null && r.getNoDueDate())
                .assignedEmployeeIds(r.getAssignedEmployeeIds())
                .description(r.getDescription())
                .milestoneId(r.getMilestoneId())
                .priority(r.getPriority() == null ? TaskPriority.MEDIUM : TaskPriority.valueOf(r.getPriority()))
                .isPrivate(r.getIsPrivate() != null && r.getIsPrivate())
                .timeEstimate(r.getTimeEstimate())
                .timeEstimateMinutes(r.getTimeEstimateMinutes())
                .isDependent(r.getIsDependent() != null && r.getIsDependent())
                .dependentTaskId(r.getDependentTaskId())
                .build();
        return t;
    }

    public static TaskDto toDto(Task e) {
        if (e == null) return null;
        TaskDto d = new TaskDto();
        d.setId(e.getId());
        d.setTitle(e.getTitle());
        d.setCategoryId(e.getCategoryId());
        d.setProjectId(e.getProjectId());
        d.setStartDate(e.getStartDate());
        d.setDueDate(e.getDueDate());
        d.setNoDueDate(e.isNoDueDate());

        // ✅ UPDATED: Map TaskStage
        d.setTaskStage(e.getTaskStage());
        d.setTaskStageId(e.getTaskStage() != null ? e.getTaskStage().getId() : null);

//        // ✅ For backward compatibility
//        d.setStatusId(e.getTaskStage() != null ? e.getTaskStage().getId() : null);
//        d.setStatusEnum(e.getStatusEnum());

        d.setAssignedEmployeeIds(e.getAssignedEmployeeIds());
        d.setDescription(e.getDescription());
        // labels, attachments, milestone, assignedEmployees must be set by service after fetching related data
        d.setMilestoneId(e.getMilestoneId());
        d.setPriority(e.getPriority() == null ? null : e.getPriority().name());
        d.setIsPrivate(e.isPrivate());
        d.setTimeEstimate(e.getTimeEstimate());
        d.setTimeEstimateMinutes(e.getTimeEstimateMinutes());
        d.setIsDependent(e.isDependent());
        d.setDependentTaskId(e.getDependentTaskId());
        d.setCreatedBy(e.getCreatedBy());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedBy(e.getUpdatedBy());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    public static Task toEntitys(EmployeeTaskCreateRequest req) {
        if (req == null) return null;
        Task t = Task.builder()
                .title(req.getTitle())
                .categoryId(req.getCategoryId())
                .projectId(req.getProjectId())
                .startDate(req.getStartDate())
                .dueDate(req.getDueDate())
                .noDueDate(req.getNoDueDate() != null && req.getNoDueDate())
                .assignedEmployeeIds(req.getAssignedEmployeeIds())
                .description(req.getDescription())
                .milestoneId(req.getMilestoneId())
                .priority(req.getPriority() == null ? TaskPriority.MEDIUM : TaskPriority.valueOf(req.getPriority()))
                .isPrivate(req.getIsPrivate() != null && req.getIsPrivate())
                .timeEstimate(req.getTimeEstimate())
                .timeEstimateMinutes(req.getTimeEstimateMinutes())
                .isDependent(req.getIsDependent() != null && req.getIsDependent())
                .dependentTaskId(req.getDependentTaskId())
                .build();
        return t;
    }
}
