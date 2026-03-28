package com.erp.project_service.dto.task;

import com.erp.project_service.dto.common.EmployeeMetaDto;
import com.erp.project_service.dto.file.FileMetaDto;
import com.erp.project_service.dto.milestone.MilestoneDto;
import com.erp.project_service.entity.Task;
import com.erp.project_service.entity.TaskCategory;
import com.erp.project_service.entity.TaskStage;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
public class TaskDto {
    private Long id;
    private String title;
    private TaskCategory categoryId;
    private Long projectId;
    private String projectShortCode; // ✅ NEW FIELD - Project ka shortcode
    private String projectName;
    private LocalDate startDate;
    private LocalDate dueDate;
    private Boolean noDueDate;
    // ✅ UPDATED: Use TaskStage instead of statusId/statusEnum
    private TaskStage taskStage;
    private Long taskStageId;

    // ✅ KEEP for backward compatibility but mark as deprecated
//    private Long statusId;
//    private String statusEnum;
    private Set<String> assignedEmployeeIds;
    private List<EmployeeMetaDto> assignedEmployees;// enriched by service
    private String description;
    private List<com.erp.project_service.dto.task.LabelDto> labels;
    private MilestoneDto milestone;
    private Long milestoneId;
    private String priority;
    private Boolean isPrivate;
    private Boolean timeEstimate;
    private Integer timeEstimateMinutes;
    private Boolean isDependent;
    private Long dependentTaskId;
    private List<FileMetaDto> attachments;
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;

    //Task pinned
    // 🔽 per-user view state
    private Boolean pinned;     // true/false
    private Instant pinnedAt;

    private Long hoursLoggedMinutes; // total minutes logged on task
    private Double hoursLogged;      // minutes/60.0, rounded to 2 decimals
    private LocalDate completedOn;
}
