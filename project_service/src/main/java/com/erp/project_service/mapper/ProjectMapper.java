package com.erp.project_service.mapper;

import com.erp.project_service.dto.project.ProjectCreateRequest;
import com.erp.project_service.dto.project.ProjectDto;
import com.erp.project_service.dto.project.ProjectUpdateRequest;
import com.erp.project_service.entity.Project;
import com.erp.project_service.entity.ProjectStatus;

import java.util.HashSet;

public final class ProjectMapper {
    private ProjectMapper() {}

    public static Project toEntity(ProjectCreateRequest r) {
        if (r == null) return null;
        Project p = Project.builder()
                .shortCode(r.getShortCode())
                .name(r.getName())
                .startDate(r.getStartDate())
                .deadline(r.getDeadline())
                .noDeadline(r.getNoDeadline() != null && r.getNoDeadline())
                .category(r.getCategory())
                .department(r.getDepartment())
                .clientId(r.getClientId())
                .summary(r.getSummary())
                .tasksNeedAdminApproval(r.getTasksNeedAdminApproval() != null && r.getTasksNeedAdminApproval())
                .currency(r.getCurrency())
                .budget(r.getBudget())
                .assignedEmployeeIds(r.getAssignedEmployeeIds() != null ?
                        r.getAssignedEmployeeIds() : new HashSet<>())
                .hoursEstimate(r.getHoursEstimate())
                .allowManualTimeLogs(r.getAllowManualTimeLogs() != null && r.getAllowManualTimeLogs())
                .build();
        return p;
    }

    public static ProjectDto toDto(Project e) {
        if (e == null) return null;
        return ProjectDto.builder()
                .id(e.getId())
                .shortCode(e.getShortCode())
                .name(e.getName())
                .startDate(e.getStartDate())
                .deadline(e.getDeadline())
                .noDeadline(e.isNoDeadline())
                .category(e.getCategory())
                .department(e.getDepartment())
                .clientId(e.getClientId())
                .summary(e.getSummary())
                .tasksNeedAdminApproval(e.isTasksNeedAdminApproval())
                .currency(e.getCurrency())
                .budget(e.getBudget())
                .hoursEstimate(e.getHoursEstimate())
                .allowManualTimeLogs(e.isAllowManualTimeLogs())
                .addedBy(e.getAddedBy())
                .assignedEmployeeIds(e.getAssignedEmployeeIds())
                .projectAdminId(e.getProjectAdminId())
                .projectStatus(e.getProjectStatus() == null ? null : e.getProjectStatus().name())
                .progressPercent(e.getProgressPercent())
                .calculateProgressThroughTasks(e.isCalculateProgressThroughTasks())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .updatedBy(e.getUpdatedBy())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    // merge update request into existing entity (for service layer)
    public static void applyUpdate(ProjectUpdateRequest req, Project project) {
        // Basic project info
        if (req.getName() != null) project.setName(req.getName());
        if (req.getStartDate() != null) project.setStartDate(req.getStartDate());
        if (req.getDeadline() != null) project.setDeadline(req.getDeadline());
        if (req.getNoDeadline() != null) project.setNoDeadline(req.getNoDeadline());
        if (req.getCategory() != null) project.setCategory(req.getCategory());
        if (req.getDepartment() != null) project.setDepartment(req.getDepartment());
        if (req.getClientId() != null) project.setClientId(req.getClientId());
        if (req.getSummary() != null) project.setSummary(req.getSummary());
        if (req.getTasksNeedAdminApproval() != null) project.setTasksNeedAdminApproval(req.getTasksNeedAdminApproval());
        if (req.getCurrency() != null) project.setCurrency(req.getCurrency());
        if (req.getBudget() != null) project.setBudget(req.getBudget());
        if (req.getHoursEstimate() != null) project.setHoursEstimate(req.getHoursEstimate());
        if (req.getAllowManualTimeLogs() != null) project.setAllowManualTimeLogs(req.getAllowManualTimeLogs());

        // Additional update fields
        if (req.getAssignedEmployeeIds() != null) project.setAssignedEmployeeIds(req.getAssignedEmployeeIds());
        if (req.getProjectStatus() != null) project.setProjectStatus(ProjectStatus.valueOf(req.getProjectStatus()));
        if (req.getProgressPercent() != null) project.setProgressPercent(req.getProgressPercent());
        if (req.getCalculateProgressThroughTasks() != null) project.setCalculateProgressThroughTasks(req.getCalculateProgressThroughTasks());
    }
}
