package com.erp.project_service.dto.task;

import com.erp.project_service.entity.TaskCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Set;

@Data
public class TaskCreateRequest {

    @NotBlank
    private String title;

    private TaskCategory categoryId;

    @NotNull
    private Long projectId;

    private LocalDate startDate;
    private LocalDate dueDate;
    private Boolean noDueDate = false;

    // statusId is optional during create; for admin create they may set it; employee-created tasks will default to WAITING
    private Long taskStageId;

    private MultipartFile taskFile;

    // employee ids
    private Set<String> assignedEmployeeIds;

    private String description;
    private Set<Long> labelIds;
    private Long milestoneId;
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private Boolean isPrivate = false;
    private Boolean timeEstimate = false;

    // time estimate in minutes
    private Integer timeEstimateMinutes;

    private Boolean isDependent = false;
    private Long dependentTaskId;
}
