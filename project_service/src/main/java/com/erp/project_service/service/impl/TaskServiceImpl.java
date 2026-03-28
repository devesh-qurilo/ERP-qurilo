//package com.erp.project_service.service.impl;
//
//import com.erp.project_service.client.EmployeeClient;
//import com.erp.project_service.dto.common.EmployeeMetaDto;
//import com.erp.project_service.dto.file.FileMetaDto;
//import com.erp.project_service.dto.milestone.MilestoneDto;
//import com.erp.project_service.dto.task.*;
//import com.erp.project_service.entity.*;
//import com.erp.project_service.exception.BadRequestException;
//import com.erp.project_service.exception.NotFoundException;
//import com.erp.project_service.mapper.TaskMapper;
//import com.erp.project_service.repository.*;
//import com.erp.project_service.security.SecurityUtils;
//import com.erp.project_service.service.TaskUserStateService;
//import com.erp.project_service.service.interfaces.FileService;
//import com.erp.project_service.service.interfaces.SubtaskService;
//import com.erp.project_service.service.interfaces.TaskService;
//import com.erp.project_service.service.interfaces.ProjectActivityService;
//import com.erp.project_service.service.notification.NotificationHelperService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.Instant;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.ZoneId; // NEW: used for completedOn timezone
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * TaskServiceImpl - updated to support:
// *  1) hours logged aggregation from TimeLog entries (enriched into DTO)
// *  2) completedOn date that is set when TaskStage becomes a completed-like stage
// *
// *   All changes are annotated with // NEW or // CHANGED
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class TaskServiceImpl implements TaskService {
//
//    private final TaskRepository taskRepository;
//    private final ProjectActivityService activityService;
//    private final ProjectRepository projectRepository;
//    private final FileService fileService;
//    private final FileMetaRepository fileMetaRepository;
//    private final EmployeeClient employeeClient;
//    private final LabelRepository labelRepository;
//    private final ProjectMilestoneRepository milestoneRepository;
//    private final TaskCategoryRepository taskCategoryRepository;
//    private final TaskStageRepository taskStageRepository;
//    private final SubtaskService subtaskService;
//    private final SubtaskRepository subtaskRepository;
//    private final NotificationHelperService notificationHelper;
//    private final TaskUserStateService taskUserStateService;
//
//    // NEW: TimeLog repository dependency used for hours logged aggregation
//    private final TimeLogRepository timeLogRepository; // NEW
//
//    @Override
//    @Transactional
//    public TaskDto create(Long projectId, TaskCreateRequest req, String createdBy) {
//        if (req == null) throw new BadRequestException("request body required");
//        if (req.getProjectId() == null) throw new BadRequestException("projectId required in request");
//        if (!projectId.equals(req.getProjectId())) throw new BadRequestException("projectId mismatch");
//
//        Project project = projectRepository.findById(projectId)
//                .orElseThrow(() -> new NotFoundException("Project not found"));
//
//        Task t = TaskMapper.toEntity(req);
//        t.setCreatedBy(createdBy);
//
//        // Category handling (if provided)
//        if (req.getCategoryId() != null && req.getCategoryId().getId() != null) {
//            TaskCategory category = taskCategoryRepository.findById(req.getCategoryId().getId())
//                    .orElseThrow(() -> new BadRequestException("Category not found: " + req.getCategoryId().getId()));
//            t.setCategoryId(category);
//        }
//
//        if (t.getAssignedEmployeeIds() == null) t.setAssignedEmployeeIds(new HashSet<>());
//
//        boolean isAdmin = SecurityUtils.isAdmin();
//
//        TaskStage requestedTaskStage = null;
//        if (req.getTaskStageId() != null) {
//            requestedTaskStage = taskStageRepository.findById(req.getTaskStageId())
//                    .orElseThrow(() -> new BadRequestException("TaskStage not found: " + req.getTaskStageId()));
//        }
//
//        if (isAdmin) {
//            // Admin-created -> full freedom
//            if (requestedTaskStage != null) {
//                t.setTaskStage(requestedTaskStage);
//                if (requestedTaskStage.getName() != null && !requestedTaskStage.getName().isBlank()) {
//                    t.setStatusEnum(requestedTaskStage.getName());
//                }
//            } else {
//                if (t.getStatusEnum() == null || t.getStatusEnum().isBlank()) {
//                    t.setStatusEnum("INCOMPLETE");
//                }
//                if (t.getTaskStage() == null) {
//                    TaskStage defaultStage = taskStageRepository.findFirstByOrderByIdAsc().orElse(null);
//                    if (defaultStage != null) t.setTaskStage(defaultStage);
//                }
//            }
//            t.setApprovedByAdmin(true);
//            t.setApprovedAt(java.time.Instant.now());
//            t.setApprovedBy(createdBy);
//
//        } else {
//            // Creator is employee
//            if (project.isTasksNeedAdminApproval()) {
//                // Force WAITING state regardless of provided taskStageId
//                t.setStatusEnum("WAITING");
//                TaskStage waitingStage = taskStageRepository.findByName("WAITING").orElse(null);
//                if (waitingStage != null) t.setTaskStage(waitingStage);
//                t.setApprovedByAdmin(false);
//            } else {
//                // Project doesn't require admin approval -> honor employee-provided stage (if any)
//                if (requestedTaskStage != null) {
//                    t.setTaskStage(requestedTaskStage);
//                    if (requestedTaskStage.getName() != null && !requestedTaskStage.getName().isBlank()) {
//                        t.setStatusEnum(requestedTaskStage.getName());
//                    }
//                } else {
//                    if (t.getStatusEnum() == null || t.getStatusEnum().isBlank()) {
//                        t.setStatusEnum("INCOMPLETE");
//                    }
//                    if (t.getTaskStage() == null) {
//                        TaskStage defaultStage = taskStageRepository.findFirstByOrderByIdAsc().orElse(null);
//                        if (defaultStage != null) t.setTaskStage(defaultStage);
//                    }
//                }
//                // Mark as implicitly approved since project doesn't require admin approval
//                t.setApprovedByAdmin(true);
//                t.setApprovedAt(java.time.Instant.now());
//                t.setApprovedBy(createdBy);
//            }
//        }
//
//        // ----- Additional validations -----
//        if (t.getMilestoneId() != null) {
//            ProjectMilestone milestone = milestoneRepository.findById(t.getMilestoneId())
//                    .orElseThrow(() -> new BadRequestException("Milestone not found: " + t.getMilestoneId()));
//            if (!projectId.equals(milestone.getProjectId())) {
//                throw new BadRequestException("Milestone does not belong to project");
//            }
//        }
//
//        if (req.getLabelIds() != null && !req.getLabelIds().isEmpty()) {
//            List<Label> labels = labelRepository.findAllById(req.getLabelIds());
//            List<Label> validLabels = labels.stream()
//                    .filter(label -> label.getProjectId() != null && label.getProjectId().equals(projectId))
//                    .collect(Collectors.toList());
//            t.setLabels(new HashSet<>(validLabels));
//        }
//
//        if (Boolean.TRUE.equals(t.isDependent()) && t.getDependentTaskId() != null) {
//            if (t.getDependentTaskId().equals(t.getId())) throw new BadRequestException("Task cannot depend on itself");
//            Task dependent = taskRepository.findById(t.getDependentTaskId())
//                    .orElseThrow(() -> new BadRequestException("Dependent task not found: " + t.getDependentTaskId()));
//            if (!projectId.equals(dependent.getProjectId())) {
//                throw new BadRequestException("Dependent task does not belong to same project");
//            }
//        }
//
//        // only keep employees assigned to project
//        if (t.getAssignedEmployeeIds() != null && !t.getAssignedEmployeeIds().isEmpty()) {
//            List<String> notAssignedToProject = new ArrayList<>();
//            List<String> validEmployees = new ArrayList<>();
//            for (String empId : t.getAssignedEmployeeIds()) {
//                if (project.getAssignedEmployeeIds() != null && project.getAssignedEmployeeIds().contains(empId)) {
//                    validEmployees.add(empId);
//                } else {
//                    notAssignedToProject.add(empId);
//                }
//            }
//            t.setAssignedEmployeeIds(new HashSet<>(validEmployees));
//            if (!notAssignedToProject.isEmpty()) {
//                log.warn("Employees {} are not assigned to project {}. They were not assigned to the task.",
//                        notAssignedToProject, projectId);
//            }
//        }
//
//        if (req.getTimeEstimate() == true){
//            if (req.getTimeEstimateMinutes() != null) {
//                t.setTimeEstimateMinutes(req.getTimeEstimateMinutes());
//            }
//        }
//
//        // NEW: Set completedOn on create if the assigned TaskStage is a completed-like stage
//        try {
//            if (t.getTaskStage() != null && isStageCompletedByName(t.getTaskStage().getName())) {
//                t.setCompletedOn(LocalDate.now(ZoneId.of("Asia/Kolkata"))); // NEW
//            } else {
//                t.setCompletedOn(null); // NEW
//            }
//        } catch (Exception e) {
//            log.warn("Failed to set completedOn during create: {}", e.getMessage());
//        }
//
//        // Persist
//        Task saved = taskRepository.save(t);
//
//        // File upload non-fatal
//        if (req.getTaskFile() != null && !req.getTaskFile().isEmpty()) {
//            try {
//                uploadFileInNewTransaction(saved.getId(), req.getTaskFile(), createdBy);
//            } catch (Exception e) {
//                log.error("Failed to upload task file for task: " + saved.getId(), e.getMessage());
//            }
//        }
//
//        // NEW: Send custom notifications to assigned employees
//        if (req.getAssignedEmployeeIds() != null && !req.getAssignedEmployeeIds().isEmpty()) {
//            sendTaskAssignmentNotifications(createdBy, req.getAssignedEmployeeIds(), req.getTitle(), saved.getId(), projectId, "CREATED");
//        }
//
//        activityService.record(projectId, createdBy, "TASK_CREATED", String.valueOf(saved.getId()));
//
//        return enrichTaskDto(saved);
//    }
//
//    @Override
//    @Transactional
//    public TaskDto creates(Long projectId, EmployeeTaskCreateRequest req, String createdBy) {
//        if (req == null) throw new BadRequestException("request body required");
//        if (req.getProjectId() == null) throw new BadRequestException("projectId required in request");
//        if (!projectId.equals(req.getProjectId())) throw new BadRequestException("projectId mismatch");
//
//        Project project = projectRepository.findById(projectId)
//                .orElseThrow(() -> new NotFoundException("Project not found"));
//
//        Task t = TaskMapper.toEntitys(req);
//        t.setCreatedBy(createdBy);
//
//        // Category handling (if provided)
//        if (req.getCategoryId() != null && req.getCategoryId().getId() != null) {
//            TaskCategory category = taskCategoryRepository.findById(req.getCategoryId().getId())
//                    .orElseThrow(() -> new BadRequestException("Category not found: " + req.getCategoryId().getId()));
//            t.setCategoryId(category);
//        }
//
//        if (t.getAssignedEmployeeIds() == null) t.setAssignedEmployeeIds(new HashSet<>());
//
//        boolean isAdmin = SecurityUtils.isAdmin();
//
//        TaskStage requestedTaskStage = null;
//        if (req.getTaskStageId() != null) {
//            requestedTaskStage = taskStageRepository.findById(req.getTaskStageId())
//                    .orElseThrow(() -> new BadRequestException("TaskStage not found: " + req.getTaskStageId()));
//        }
//
//        if (isAdmin) {
//            // Admin-created -> full freedom
//            if (requestedTaskStage != null) {
//                t.setTaskStage(requestedTaskStage);
//                if (requestedTaskStage.getName() != null && !requestedTaskStage.getName().isBlank()) {
//                    t.setStatusEnum(requestedTaskStage.getName());
//                }
//            } else {
//                if (t.getStatusEnum() == null || t.getStatusEnum().isBlank()) {
//                    t.setStatusEnum("Incomplete");
//                }
//                if (t.getTaskStage() == null) {
//                    TaskStage defaultStage = taskStageRepository.findFirstByOrderByIdAsc().orElse(null);
//                    if (defaultStage != null) t.setTaskStage(defaultStage);
//                }
//            }
//            t.setApprovedByAdmin(true);
//            t.setApprovedAt(java.time.Instant.now());
//            t.setApprovedBy(createdBy);
//
//        } else {
//            // Creator is employee
//            if (project.isTasksNeedAdminApproval()) {
//                // Force WAITING state regardless of provided taskStageId
//                t.setStatusEnum("Waiting");
//                TaskStage waitingStage = taskStageRepository.findByName("Waiting").orElse(null);
//                if (waitingStage != null) t.setTaskStage(waitingStage);
//                t.setApprovedByAdmin(false);
//            } else {
//                // Project doesn't require admin approval -> honor employee-provided stage (if any)
//                if (requestedTaskStage != null) {
//                    t.setTaskStage(requestedTaskStage);
//                    if (requestedTaskStage.getName() != null && !requestedTaskStage.getName().isBlank()) {
//                        t.setStatusEnum(requestedTaskStage.getName());
//                    }
//                } else {
//                    if (t.getStatusEnum() == null || t.getStatusEnum().isBlank()) {
//                        t.setStatusEnum("Incomplete");
//                    }
//                    if (t.getTaskStage() == null) {
//                        TaskStage defaultStage = taskStageRepository.findFirstByOrderByIdAsc().orElse(null);
//                        if (defaultStage != null) t.setTaskStage(defaultStage);
//                    }
//                }
//                // Mark as implicitly approved since project doesn't require admin approval
//                t.setApprovedByAdmin(true);
//                t.setApprovedAt(java.time.Instant.now());
//                t.setApprovedBy(createdBy);
//            }
//        }
//
//        // ----- Additional validations -----
//        if (t.getMilestoneId() != null) {
//            ProjectMilestone milestone = milestoneRepository.findById(t.getMilestoneId())
//                    .orElseThrow(() -> new BadRequestException("Milestone not found: " + t.getMilestoneId()));
//            if (!projectId.equals(milestone.getProjectId())) {
//                throw new BadRequestException("Milestone does not belong to project");
//            }
//        }
//
//        if (req.getLabelIds() != null && !req.getLabelIds().isEmpty()) {
//            List<Label> labels = labelRepository.findAllById(req.getLabelIds());
//            List<Label> validLabels = labels.stream()
//                    .filter(label -> label.getProjectId() != null && label.getProjectId().equals(projectId))
//                    .collect(Collectors.toList());
//            t.setLabels(new HashSet<>(validLabels));
//        }
//
//        if (Boolean.TRUE.equals(t.isDependent()) && t.getDependentTaskId() != null) {
//            if (t.getDependentTaskId().equals(t.getId())) throw new BadRequestException("Task cannot depend on itself");
//            Task dependent = taskRepository.findById(t.getDependentTaskId())
//                    .orElseThrow(() -> new BadRequestException("Dependent task not found: " + t.getDependentTaskId()));
//            if (!projectId.equals(dependent.getProjectId())) {
//                throw new BadRequestException("Dependent task does not belong to same project");
//            }
//        }
//
//        // only keep employees assigned to project
//        if (t.getAssignedEmployeeIds() != null && !t.getAssignedEmployeeIds().isEmpty()) {
//            List<String> notAssignedToProject = new ArrayList<>();
//            List<String> validEmployees = new ArrayList<>();
//            for (String empId : t.getAssignedEmployeeIds()) {
//                if (project.getAssignedEmployeeIds() != null && project.getAssignedEmployeeIds().contains(empId)) {
//                    validEmployees.add(empId);
//                } else {
//                    notAssignedToProject.add(empId);
//                }
//            }
//            t.setAssignedEmployeeIds(new HashSet<>(validEmployees));
//            if (!notAssignedToProject.isEmpty()) {
//                log.warn("Employees {} are not assigned to project {}. They were not assigned to the task.",
//                        notAssignedToProject, projectId);
//            }
//        }
//
//        if (req.getTimeEstimate() == true){
//            if (req.getTimeEstimateMinutes() != null) {
//                t.setTimeEstimateMinutes(req.getTimeEstimateMinutes());
//            }
//        }
//
//        // NEW: Set completedOn on create(s) if the chosen stage is completed-like
//        try {
//            if (t.getTaskStage() != null && isStageCompletedByName(t.getTaskStage().getName())) {
//                t.setCompletedOn(LocalDate.now(ZoneId.of("Asia/Kolkata"))); // NEW
//            } else {
//                t.setCompletedOn(null); // NEW
//            }
//        } catch (Exception e) {
//            log.warn("Failed to set completedOn during creates: {}", e.getMessage());
//        }
//
//        // Persist
//        Task saved = taskRepository.save(t);
//
//        // File upload non-fatal
//        if (req.getTaskFile() != null && !req.getTaskFile().isEmpty()) {
//            try {
//                uploadFileInNewTransaction(saved.getId(), req.getTaskFile(), createdBy);
//            } catch (Exception e) {
//                log.error("Failed to upload task file for task: " + saved.getId(), e);
//            }
//        }
//
//        activityService.record(projectId, createdBy, "TASK_CREATED", String.valueOf(saved.getId()));
//
//        return enrichTaskDto(saved);
//    }
//
//    @Override
//    public TaskDto get(Long projectId, Long taskId, String requesterId) {
//        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
//        if (!t.getProjectId().equals(projectId)) throw new NotFoundException("Task not found in project");
//
//        // Return enriched TaskDto
//        return enrichTaskDto(t);
//    }
//
//    @Override
//    @Transactional
//    public TaskDto update(Long projectId, Long taskId, TaskCreateRequest req, String updatedBy) {
//        Task existingTask = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
//        if (!existingTask.getProjectId().equals(projectId)) throw new BadRequestException("project mismatch");
//
//        // ensure project exists
//        Project project = projectRepository.findById(projectId)
//                .orElseThrow(() -> new NotFoundException("Project not found"));
//
//        // Apply updates
//        if (req.getTitle() != null) existingTask.setTitle(req.getTitle());
//        if (req.getDescription() != null) existingTask.setDescription(req.getDescription());
//        if (req.getStartDate() != null) existingTask.setStartDate(req.getStartDate());
//        if (req.getDueDate() != null) existingTask.setDueDate(req.getDueDate());
//        if (req.getNoDueDate() != null) existingTask.setNoDueDate(req.getNoDueDate());
//
//        // Update TaskStage from taskStageId
//        if (req.getTaskStageId() != null) {
//            TaskStage taskStage = taskStageRepository.findById(req.getTaskStageId())
//                    .orElseThrow(() -> new BadRequestException("TaskStage not found: " + req.getTaskStageId()));
//            existingTask.setTaskStage(taskStage);
//        }
//
//        if (req.getMilestoneId() != null) existingTask.setMilestoneId(req.getMilestoneId());
//        if (req.getPriority() != null) {
//            try {
//                existingTask.setPriority(TaskPriority.valueOf(req.getPriority()));
//            } catch (IllegalArgumentException e) {
//                throw new BadRequestException("Invalid priority: " + req.getPriority());
//            }
//        }
//        if (req.getIsPrivate() != null) existingTask.setPrivate(req.getIsPrivate());
//        if (req.getTimeEstimate() != null) existingTask.setTimeEstimate(req.getTimeEstimate());
//        if (req.getTimeEstimateMinutes() != null) existingTask.setTimeEstimateMinutes(req.getTimeEstimateMinutes());
//        if (req.getIsDependent() != null) existingTask.setDependent(req.getIsDependent());
//        if (req.getDependentTaskId() != null) existingTask.setDependentTaskId(req.getDependentTaskId());
//
//        // Update category properly - ONLY ONCE
//        if (req.getCategoryId() != null) {
//            if (req.getCategoryId().getId() != null) {
//                // Fetch complete category from database
//                TaskCategory category = taskCategoryRepository.findById(req.getCategoryId().getId())
//                        .orElseThrow(() -> new BadRequestException("Category not found: " + req.getCategoryId().getId()));
//                existingTask.setCategoryId(category);
//                log.info("Updated category to ID: {} for task: {}", category.getId(), taskId);
//            } else {
//                // If categoryId object exists but ID is null, remove category
//                existingTask.setCategoryId(null);
//                log.info("Removed category from task: {}", taskId);
//            }
//        }
//        // If req.getCategoryId() is null, don't change the existing category
//
//        // Update assigned employees with project validation
//        if (req.getAssignedEmployeeIds() != null) {
//            List<String> notAssignedToProject = new ArrayList<>();
//            List<String> validEmployees = new ArrayList<>();
//
//            for (String empId : req.getAssignedEmployeeIds()) {
//                if (project.getAssignedEmployeeIds() != null && project.getAssignedEmployeeIds().contains(empId)) {
//                    validEmployees.add(empId);
//                } else {
//                    notAssignedToProject.add(empId);
//                }
//            }
//
//            existingTask.setAssignedEmployeeIds(new HashSet<>(validEmployees));
//
//            if (!notAssignedToProject.isEmpty()) {
//                log.warn("Employees {} are not assigned to project {}. They were not assigned to the task.",
//                        notAssignedToProject, projectId);
//            }
//        }
//
//        // Update labels if provided
//        if (req.getLabelIds() != null) {
//            try {
//                if (req.getLabelIds().isEmpty()) {
//                    // Clear labels if empty array provided
//                    existingTask.getLabels().clear();
//                    log.info("Cleared all labels from task: {}", taskId);
//                } else {
//                    // Fetch and set new labels
//                    List<Label> labels = labelRepository.findAllById(req.getLabelIds());
//
//                    // Verify all labels belong to the same project
//                    List<Label> validLabels = labels.stream()
//                            .filter(label -> label.getProjectId() != null && label.getProjectId().equals(projectId))
//                            .collect(Collectors.toList());
//
//                    // Clear existing labels and add new ones
//                    existingTask.getLabels().clear();
//                    existingTask.getLabels().addAll(validLabels);
//
//                    log.info("Updated {} labels for task {}", validLabels.size(), taskId);
//                }
//            } catch (Exception e) {
//                log.warn("Failed to update labels for task {}: {}", taskId, e.getMessage());
//            }
//        }
//
//        existingTask.setUpdatedBy(updatedBy);
//
//        // NEW/CHANGED: Update completedOn according to the new TaskStage if TaskStage was changed above
//        try {
//            if (existingTask.getTaskStage() != null) {
//                if (isStageCompletedByName(existingTask.getTaskStage().getName())) {
//                    // set completedOn to today in Asia/Kolkata
//                    existingTask.setCompletedOn(LocalDate.now(ZoneId.of("Asia/Kolkata"))); // NEW
//                } else {
//                    existingTask.setCompletedOn(null); // NEW: clear if moving away from completed
//                }
//            }
//        } catch (Exception e) {
//            log.warn("Failed to update completedOn during task update: {}", e.getMessage());
//        }
//
//        // Save and fetch fresh to avoid Hibernate proxy issues
//        Task saved = taskRepository.save(existingTask);
//
//        // Fetch fresh task with all relationships to avoid serialization issues
//        Task freshTask = taskRepository.findById(taskId)
//                .orElseThrow(() -> new NotFoundException("Task not found after update"));
//
//        // Initialize lazy collections
//        if (freshTask.getLabels() != null) {
//            freshTask.getLabels().size(); // Force initialization
//        }
//
//        // Handle file upload if present
//        if (req.getTaskFile() != null && !req.getTaskFile().isEmpty()) {
//            try {
//                fileService.uploadTaskFile(taskId, req.getTaskFile(), updatedBy);
//            } catch (Exception e) {
//                log.error("Failed to upload task file for task: " + taskId, e);
//            }
//        }
//
//        // NEW: Send custom notifications for task update
//        if (req.getAssignedEmployeeIds() != null && !req.getAssignedEmployeeIds().isEmpty()) {
//            sendTaskAssignmentNotifications(updatedBy, req.getAssignedEmployeeIds(), req.getTitle(), saved.getId(), projectId, "UPDATED");
//        }
//
//        activityService.record(projectId, updatedBy, "TASK_UPDATED", String.valueOf(saved.getId()));
//
//        return enrichTaskDto(freshTask);
//    }
//
//    private TaskDto enrichTaskDto(Task task) {
//        TaskDto dto = TaskMapper.toDto(task);
//
//        // Enrich TaskStage data
//        if (task.getTaskStage() != null) {
//            dto.setTaskStageId(task.getTaskStage().getId());
//        }
//
//        // 1. Enrich assigned employees
//        if (task.getAssignedEmployeeIds() != null && !task.getAssignedEmployeeIds().isEmpty()) {
//            try {
//                List<EmployeeMetaDto> assignedEmployees = new ArrayList<>();
//                for (String empId : task.getAssignedEmployeeIds()) {
//                    try {
//                        EmployeeMetaDto employeeMeta = employeeClient.getMeta(empId);
//                        if (employeeMeta != null) {
//                            assignedEmployees.add(employeeMeta);
//                        }
//                    } catch (Exception e) {
//                        log.debug("Failed to fetch employee meta for: {}", empId);
//                    }
//                }
//                dto.setAssignedEmployees(assignedEmployees);
//            } catch (Exception e) {
//                log.warn("Failed to enrich assigned employees: {}", e.getMessage());
//            }
//        }
//
//        // 2. Enrich labels - Check if labels are actually loaded
//        if (task.getLabels() != null) {
//            try {
//                log.debug("Task {} has {} labels", task.getId(), task.getLabels().size());
//
//                List<LabelDto> labelDtos = task.getLabels().stream()
//                        .map(label -> {
//                            LabelDto labelDto = new LabelDto();
//                            labelDto.setId(label.getId());
//                            labelDto.setName(label.getName());
//                            labelDto.setColorCode(label.getColorCode());
//                            // Add other label properties if available
//                            if (label.getProjectId() != null) {
//                                labelDto.setProjectId(label.getProjectId());
//                            }
//                            if (label.getDescription() != null) {
//                                labelDto.setDescription(label.getDescription());
//                            }
//                            return labelDto;
//                        })
//                        .collect(Collectors.toList());
//                dto.setLabels(labelDtos);
//                log.debug("Successfully enriched {} labels for task {}", labelDtos.size(), task.getId());
//            } catch (Exception e) {
//                log.warn("Failed to enrich labels for task {}: {}", task.getId(), e.getMessage());
//                dto.setLabels(new ArrayList<>());
//            }
//        } else {
//            log.debug("Task {} has null labels collection", task.getId());
//            dto.setLabels(new ArrayList<>());
//        }
//
//        // 3. Enrich milestone
//        if (task.getMilestoneId() != null) {
//            try {
//                ProjectMilestone milestone = milestoneRepository.findById(task.getMilestoneId()).orElse(null);
//                if (milestone != null) {
//                    MilestoneDto milestoneDto = new MilestoneDto();
//                    milestoneDto.setId(milestone.getId());
//                    milestoneDto.setTitle(milestone.getTitle());
//                    milestoneDto.setEndDate(milestone.getEndDate());
//                    milestoneDto.setStartDate(milestone.getStartDate());
//                    milestoneDto.setSummary(milestone.getSummary());
//                    milestoneDto.setStatus(milestone.getStatus().toString());
//                    milestoneDto.setMilestoneCost(milestone.getMilestoneCost());
//                    dto.setMilestone(milestoneDto);
//                }
//            } catch (Exception e) {
//                log.warn("Failed to enrich milestone: {}", e.getMessage());
//            }
//        }
//
//        // 4. Enrich attachments
//        try {
//            List<FileMetaDto> attachments = fileService.listTaskFiles(task.getId());
//            dto.setAttachments(attachments);
//        } catch (Exception e) {
//            log.warn("Failed to enrich attachments: {}", e.getMessage());
//        }
//
//        // ✅ NEW: Enrich project shortcode
//        if (task.getProjectId() != null) {
//            try {
//                String projectShortCode = projectRepository.findShortCodeById(task.getProjectId());
//                dto.setProjectShortCode(projectShortCode);
//                log.debug("Successfully enriched project shortcode '{}' for task {}", projectShortCode, task.getId());
//            } catch (Exception e) {
//                log.warn("Failed to enrich project shortcode for projectId {}: {}", task.getProjectId(), e.getMessage());
//                dto.setProjectShortCode(null);
//            }
//        }
//
//        // NEW: Hours logged aggregation - fetch aggregated minutes from TimeLog entries (preferred) with fallback
//        try {
//            Long minutes = null;
//            try {
//                minutes = timeLogRepository.sumDurationMinutesByTaskId(task.getId()); // NEW - preferred aggregation
//            } catch (Exception ex) {
//                // Repository method may not exist or fail; fallback to summing via findByTaskId
//                log.debug("Aggregation query failed for task {}: {}. Falling back to scanning timelogs.", task.getId(), ex.getMessage());
//                minutes = 0L;
//                try {
//                    List<TimeLog> tlogs = timeLogRepository.findByTaskId(task.getId());
//                    if (tlogs != null) {
//                        for (TimeLog tl : tlogs) {
//                            if (tl.getDurationMinutes() != null) minutes += tl.getDurationMinutes();
//                        }
//                    }
//                } catch (Exception e) {
//                    log.warn("Failed fallback timelog fetch for task {}: {}", task.getId(), e.getMessage());
//                    minutes = 0L;
//                }
//            }
//            if (minutes == null) minutes = 0L;
//            dto.setHoursLoggedMinutes(minutes);
//            double hours = Math.round((minutes / 60.0) * 100.0) / 100.0; // 2 decimal rounding
//            dto.setHoursLogged(hours);
//        } catch (Exception e) {
//            log.warn("Failed to compute hours logged for task {}: {}", task.getId(), e.getMessage());
//            dto.setHoursLoggedMinutes(0L);
//            dto.setHoursLogged(0.0);
//        }
//
//        // NEW: CompletedOn enrichment - directly map from entity field
//        try {
//            dto.setCompletedOn(task.getCompletedOn()); // NEW
//        } catch (Exception e) {
//            dto.setCompletedOn(null);
//        }
//
//        return dto;
//    }
//
//    @Override
//    @Transactional
//    public void delete(Long projectId, Long taskId, String deletedBy) {
//        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
//        if (!t.getProjectId().equals(projectId)) throw new NotFoundException("Task not found");
//
//        List<Subtask> subtasks = subtaskRepository.findByTaskId(taskId);
//        log.info("Deleting {} subtasks for task: {}", subtasks.size(), taskId);
//        subtaskRepository.deleteAll(subtasks);
//
//        List<FileMeta> taskFiles = fileMetaRepository.findByTaskId(taskId);
//        System.out.println("Deleting " + taskFiles.size() + " files for project: " + taskId);
//
//        for (FileMeta file : taskFiles) {
//            try {
//                fileService.deleteFile(file.getId(), deletedBy);
//            } catch (Exception e) {
//                System.err.println("Failed to delete file: " + file.getId() + ", error: " + e.getMessage());
//            }
//        }
//        taskRepository.deleteById(taskId);
//        activityService.record(projectId, deletedBy, "TASK_DELETED", String.valueOf(taskId));
//    }
//
//    @Override
//    public List<TaskDto> listByProject(Long projectId) {
//        List<Task> tasks = taskRepository.findByProjectId(projectId);
//        return tasks.stream()
//                .map(this::enrichTaskDto)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<TaskDto> listAssignedTo(String employeeId) {
//        return taskRepository.findByAssignedEmployeeId(employeeId, PageRequest.of(0, 1000))
//                .stream()
//                .map(this::enrichTaskDtos)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional
//    public TaskDto changeStatus(Long projectId, Long taskId, Long taskStageId, String actor) {
//        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
//
//        // Change TaskStage
//        TaskStage taskStage = taskStageRepository.findById(taskStageId)
//                .orElseThrow(() -> new BadRequestException("TaskStage not found: " + taskStageId));
//        t.setTaskStage(taskStage);
//
//        // NEW: Set/clear completedOn based on stage name
//        try {
//            if (isStageCompletedByName(taskStage.getName())) {
//                t.setCompletedOn(LocalDate.now(ZoneId.of("Asia/Kolkata"))); // NEW
//            } else {
//                t.setCompletedOn(null); // NEW
//            }
//        } catch (Exception e) {
//            log.warn("Failed to update completedOn during changeStatus: {}", e.getMessage());
//        }
//
//        Task saved = taskRepository.save(t);
//
//        // NEW: Send status change notification
//        sendTaskStatusChangeNotification(actor, t.getAssignedEmployeeIds(), t.getTitle(), taskStage.getName(), taskId);
//
//        activityService.record(projectId, actor, "TASK_STATUS_CHANGED", taskStageId.toString());
//        return TaskMapper.toDto(saved);
//    }
//
//    @Override
//    @Transactional
//    public TaskDto duplicate(Long projectId, Long taskId, String actor) {
//        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
//        Task copy = Task.builder()
//                .title(t.getTitle() + " (copy)")
//                .categoryId(t.getCategoryId())
//                .projectId(t.getProjectId())
//                .startDate(t.getStartDate())
//                .dueDate(t.getDueDate())
//                .noDueDate(t.isNoDueDate())
//                .taskStage(t.getTaskStage()) // Copy TaskStage
//                .assignedEmployeeIds(t.getAssignedEmployeeIds())
//                .description(t.getDescription())
//                .milestoneId(t.getMilestoneId())
//                .priority(t.getPriority())
//                .isPrivate(t.isPrivate())
//                .timeEstimateMinutes(t.getTimeEstimateMinutes())
//                .isDependent(t.isDependent())
//                .dependentTaskId(t.getDependentTaskId())
//                .duplicateOfTaskId(t.getId())
//                .createdBy(actor)
//                .build();
//        Task saved = taskRepository.save(copy);
//        activityService.record(projectId, actor, "TASK_DUPLICATED", taskId + "->" + saved.getId());
//        return TaskMapper.toDto(saved);
//    }
//
//    @Override
//    @Transactional
//    public TaskDto approveWaitingTask(Long projectId, Long taskId, String approver) {
//        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
//        if (!"WAITING".equalsIgnoreCase(t.getStatusEnum())) throw new BadRequestException("Task not in waiting state");
//        t.setStatusEnum("Incomplete");
//
//        // Also update TaskStage to INCOMPLETE if exists
//        try {
//            TaskStage incompleteStage = taskStageRepository.findByName("Incomplete")
//                    .orElse(null);
//            if (incompleteStage != null) {
//                t.setTaskStage(incompleteStage);
//            }
//        } catch (Exception e) {
//            log.warn("Failed to set INCOMPLETE task stage: {}", e.getMessage());
//        }
//
//        // NEW: Approving a waiting task moves it to Incomplete -> clear completedOn
//        t.setCompletedOn(null); // NEW
//
//        Task saved = taskRepository.save(t);
//
//        // NEW: Send task approval notification
//        sendTaskApprovalNotification(approver, t.getAssignedEmployeeIds(), t.getTitle(), taskId);
//
//        activityService.record(projectId, approver, "TASK_APPROVED", String.valueOf(taskId));
//        return TaskMapper.toDto(saved);
//    }
//
//    // Notification Helper
//    // NEW METHOD: Send task assignment notifications
//    private void sendTaskAssignmentNotifications(String actor, Set<String> assignedEmployeeIds, String taskTitle, Long taskId, Long projectId, String action) {
//        try {
//            String title = "";
//            String message = "";
//
//            if ("CREATED".equals(action)) {
//                title = "🎯 New Task Assigned";
//                message = String.format(
//                        "You have been assigned a new task: '%s'. " +
//                                "Task ID: %d | Project ID: %d. " +
//                                "Please review the task details and start working on it.",
//                        taskTitle, taskId, projectId
//                );
//            } else if ("UPDATED".equals(action)) {
//                title = "📝 Task Updated";
//                message = String.format(
//                        "Task '%s' has been updated. " +
//                                "Task ID: %d | Project ID: %d. " +
//                                "Please check the updated details.",
//                        taskTitle, taskId, projectId
//                );
//            }
//
//            notificationHelper.sendBulkNotification(actor, new ArrayList<>(assignedEmployeeIds), title, message, "TASK_ASSIGNMENT");
//
//            log.info("Task {} notifications sent to {} employees for task: {}", action, assignedEmployeeIds.size(), taskId);
//
//        } catch (Exception e) {
//            log.error("Failed to send task {} notifications: {}", action, e.getMessage());
//        }
//    }
//
//    // NEW METHOD: Send task status change notification
//    private void sendTaskStatusChangeNotification(String actor, Set<String> assignedEmployeeIds, String taskTitle, String newStatus, Long taskId) {
//        try {
//            String title = "🔄 Task Status Updated";
//            String message = String.format(
//                    "Task '%s' status has been changed to: %s. " +
//                            "Task ID: %d. " +
//                            "Current status reflects the latest progress.",
//                    taskTitle, newStatus, taskId
//            );
//
//            notificationHelper.sendBulkNotification(actor, new ArrayList<>(assignedEmployeeIds), title, message, "TASK_STATUS_CHANGE");
//
//            log.info("Task status change notification sent for task: {}", taskId);
//
//        } catch (Exception e) {
//            log.error("Failed to send task status change notification: {}", e.getMessage());
//        }
//    }
//
//    // NEW METHOD: Send task approval notification
//    private void sendTaskApprovalNotification(String approver, Set<String> assignedEmployeeIds, String taskTitle, Long taskId) {
//        try {
//            String title = "✅ Task Approved";
//            String message = String.format(
//                    "Your task '%s' has been approved by admin. " +
//                            "Task ID: %d. " +
//                            "You can now start working on this task.",
//                    taskTitle, taskId
//            );
//
//            notificationHelper.sendBulkNotification(approver, new ArrayList<>(assignedEmployeeIds), title, message, "TASK_APPROVAL");
//
//            log.info("Task approval notification sent for task: {}", taskId);
//
//        } catch (Exception e) {
//            log.error("Failed to send task approval notification: {}", e.getMessage());
//        }
//    }
//
//    @Override
//    public List<TaskDto> getAll() {
//        List<Task> tasks = taskRepository.findAll();
//        // ✅ FIX: Use stream to enrich each task individually
//        return tasks.stream()
//                .map(this::enrichTaskDtos)
//                .collect(Collectors.toList());
//    }
//
//    private TaskDto enrichTaskDtos(Task task) {
//        TaskDto dto = TaskMapper.toDto(task);
//
//        // Enrich TaskStage data
//        if (task.getTaskStage() != null) {
//            dto.setTaskStageId(task.getTaskStage().getId());
//        }
//
//        // 1. Enrich assigned employees
//        if (task.getAssignedEmployeeIds() != null && !task.getAssignedEmployeeIds().isEmpty()) {
//            try {
//                List<EmployeeMetaDto> assignedEmployees = new ArrayList<>();
//                for (String empId : task.getAssignedEmployeeIds()) {
//                    try {
//                        EmployeeMetaDto employeeMeta = employeeClient.getMeta(empId);
//                        if (employeeMeta != null) {
//                            assignedEmployees.add(employeeMeta);
//                        }
//                    } catch (Exception e) {
//                        log.debug("Failed to fetch employee meta for: {}", empId);
//                    }
//                }
//                dto.setAssignedEmployees(assignedEmployees);
//            } catch (Exception e) {
//                log.warn("Failed to enrich assigned employees: {}", e.getMessage());
//            }
//        }
//
//        // 2. Enrich labels
//        if (task.getLabels() != null) {
//            try {
//                log.debug("Task {} has {} labels", task.getId(), task.getLabels().size());
//                List<LabelDto> labelDtos = task.getLabels().stream()
//                        .map(label -> {
//                            LabelDto labelDto = new LabelDto();
//                            labelDto.setId(label.getId());
//                            labelDto.setName(label.getName());
//                            labelDto.setColorCode(label.getColorCode());
//                            if (label.getProjectId() != null) labelDto.setProjectId(label.getProjectId());
//                            if (label.getDescription() != null) labelDto.setDescription(label.getDescription());
//                            return labelDto;
//                        })
//                        .collect(Collectors.toList());
//                dto.setLabels(labelDtos);
//                log.debug("Successfully enriched {} labels for task {}", labelDtos.size(), task.getId());
//            } catch (Exception e) {
//                log.warn("Failed to enrich labels for task {}: {}", task.getId(), e.getMessage());
//                dto.setLabels(new ArrayList<>());
//            }
//        } else {
//            log.debug("Task {} has null labels collection", task.getId());
//            dto.setLabels(new ArrayList<>());
//        }
//
//        // 3. Enrich milestone
//        if (task.getMilestoneId() != null) {
//            try {
//                ProjectMilestone milestone = milestoneRepository.findById(task.getMilestoneId()).orElse(null);
//                if (milestone != null) {
//                    MilestoneDto milestoneDto = new MilestoneDto();
//                    milestoneDto.setId(milestone.getId());
//                    milestoneDto.setTitle(milestone.getTitle());
//                    milestoneDto.setEndDate(milestone.getEndDate());
//                    milestoneDto.setStartDate(milestone.getStartDate());
//                    milestoneDto.setSummary(milestone.getSummary());
//                    milestoneDto.setStatus(milestone.getStatus().toString());
//                    milestoneDto.setMilestoneCost(milestone.getMilestoneCost());
//                    dto.setMilestone(milestoneDto);
//                }
//            } catch (Exception e) {
//                log.warn("Failed to enrich milestone: {}", e.getMessage());
//            }
//        }
//
//        // 4. Enrich attachments
//        try {
//            List<FileMetaDto> attachments = fileService.listTaskFiles(task.getId());
//            dto.setAttachments(attachments);
//        } catch (Exception e) {
//            log.warn("Failed to enrich attachments: {}", e.getMessage());
//        }
//
//        // ✅ NEW: Enrich project shortcode
//        if (task.getProjectId() != null) {
//            try {
//                String projectShortCode = projectRepository.findShortCodeById(task.getProjectId());
//                dto.setProjectShortCode(projectShortCode);
//                log.debug("Successfully enriched project shortcode '{}' for task {}", projectShortCode, task.getId());
//            } catch (Exception e) {
//                log.warn("Failed to enrich project shortcode for projectId {}: {}", task.getProjectId(), e.getMessage());
//                dto.setProjectShortCode(null);
//            }
//        }
//
//        // 5. ✅ Enrich pinned state for current user
//        try {
//            String currentUser = SecurityUtils.getCurrentUserId();
//            if (currentUser != null) {
//                Instant pinnedAt = taskUserStateService.getPinnedAtForUser(task.getId(), currentUser);
//                dto.setPinned(pinnedAt != null);
//                dto.setPinnedAt(pinnedAt);
//            } else {
//                dto.setPinned(false);
//                dto.setPinnedAt(null);
//            }
//        } catch (Exception e) {
//            log.debug("Failed to set pinned state for task {}: {}", task.getId(), e.getMessage());
//            dto.setPinned(false);
//            dto.setPinnedAt(null);
//        }
//
//        // NEW: Hours logged aggregation (same logic as enrichTaskDto)
//        try {
//            Long minutes = null;
//            try {
//                minutes = timeLogRepository.sumDurationMinutesByTaskId(task.getId());
//            } catch (Exception ex) {
//                minutes = 0L;
//                try {
//                    List<TimeLog> tlogs = timeLogRepository.findByTaskId(task.getId());
//                    if (tlogs != null) {
//                        for (TimeLog tl : tlogs) {
//                            if (tl.getDurationMinutes() != null) minutes += tl.getDurationMinutes();
//                        }
//                    }
//                } catch (Exception e) {
//                    log.warn("Fallback timelog fetch failed for task {}: {}", task.getId(), e.getMessage());
//                    minutes = 0L;
//                }
//            }
//            if (minutes == null) minutes = 0L;
//            dto.setHoursLoggedMinutes(minutes);
//            double hours = Math.round((minutes / 60.0) * 100.0) / 100.0;
//            dto.setHoursLogged(hours);
//        } catch (Exception e) {
//            dto.setHoursLoggedMinutes(0L);
//            dto.setHoursLogged(0.0);
//        }
//
//        // NEW: CompletedOn enrichment
//        try {
//            dto.setCompletedOn(task.getCompletedOn());
//        } catch (Exception e) {
//            dto.setCompletedOn(null);
//        }
//
//        return dto;
//    }
//
//    @Override
//    public void deleteTaskById(Long taskId, String deletedBy) {
//        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
//
//        List<FileMeta> taskFiles = fileMetaRepository.findByTaskId(taskId);
//        System.out.println("Deleting " + taskFiles.size() + " files for task: " + taskId);
//
//        for (FileMeta file : taskFiles) {
//            try {
//                fileService.deleteFile(file.getId(), deletedBy);
//            } catch (Exception e) {
//                System.err.println("Failed to delete file: " + file.getId() + ", error: " + e.getMessage());
//            }
//        }
//
//        // ✅ FIX: Get project ID from the task entity we already have
//        Long projectId = t.getProjectId();
//
//        taskRepository.deleteById(taskId);
//        activityService.record(projectId, deletedBy, "TASK_DELETED", String.valueOf(taskId));
//    }
//
//    @Override
//    public List<TaskDto> getAllWaititngTask(String Status) {
//        return taskRepository.findByStatusName(Status).stream().map(TaskMapper::toDto).collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public EmployeeTaskCountDto getAssignedTaskCount(String employeeId) {
//        if (employeeId == null || employeeId.trim().isEmpty()) {
//            return new EmployeeTaskCountDto(employeeId, 0L);
//        }
//
//        long count = 0L;
//
//        try {
//            // Preferred: direct repo count method if available (fast)
//            try {
//                java.lang.reflect.Method m = taskRepository.getClass().getMethod("countByAssignedEmployeeId", String.class);
//                Object res = m.invoke(taskRepository, employeeId);
//                if (res instanceof Number) {
//                    count = ((Number) res).longValue();
//                } else {
//                    count = 0L;
//                }
//            } catch (NoSuchMethodException ignored) {
//                // Fallback: use Page and read totalElements (executes optimized count query under the hood)
//                org.springframework.data.domain.Page<Task> page =
//                        taskRepository.findByAssignedEmployeeId(employeeId, org.springframework.data.domain.PageRequest.of(0, 1));
//                count = page.getTotalElements();
//            }
//        } catch (Exception ex) {
//            log.warn("Failed to compute task count via optimized path, falling back to scanning. Error: {}", ex.getMessage());
//            // final fallback (only if all else fails)
//            count = taskRepository.findAll().stream()
//                    .filter(t -> t.getAssignedEmployeeIds() != null && t.getAssignedEmployeeIds().contains(employeeId))
//                    .count();
//        }
//
//        return new EmployeeTaskCountDto(employeeId, count);
//    }
//
//    @Transactional(readOnly = true)
//    public TaskCountsDto getAllTasksCounts() {
//        List<String> completedNames = Arrays.asList("completed", "finished", "complete", "finish");
//        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
//
//        long pending = 0L;
//        long overdue = 0L;
//
//        try {
//            pending = taskRepository.countPendingTasks(completedNames);
//            overdue = taskRepository.countOverdueTasks(completedNames, today);
//        } catch (Exception ex) {
//            log.warn("Optimized task counts failed, falling back to in-memory scan: {}", ex.getMessage());
//            // fallback
//            pending = taskRepository.findAll().stream()
//                    .filter(t -> {
//                        boolean isCompleted = false;
//                        try {
//                            if (t.getTaskStage() != null && t.getTaskStage().getName() != null) {
//                                String n = t.getTaskStage().getName().toLowerCase();
//                                isCompleted = completedNames.contains(n);
//                            }
//                        } catch (Exception ignored) {}
//                        return !isCompleted;
//                    })
//                    .count();
//
//            overdue = taskRepository.findAll().stream()
//                    .filter(t -> {
//                        boolean isCompleted = false;
//                        try {
//                            if (t.getTaskStage() != null && t.getTaskStage().getName() != null) {
//                                String n = t.getTaskStage().getName().toLowerCase();
//                                isCompleted = completedNames.contains(n);
//                            }
//                        } catch (Exception ignored) {}
//
//                        if (isCompleted) return false;
//                        if (Boolean.TRUE.equals(t.isNoDueDate())) return false;
//                        if (t.getDueDate() == null) return false;
//                        return t.getDueDate().isBefore(today);
//                    })
//                    .count();
//        }
//
//        return new TaskCountsDto(pending, overdue);
//    }
//
//    @Transactional(readOnly = true)
//    public TaskCountsDto getTasksCountsForEmployee(String employeeId) {
//        if (employeeId == null || employeeId.trim().isEmpty()) return new TaskCountsDto(0L, 0L);
//
//        List<String> completedNames = Arrays.asList("completed", "finished", "complete", "finish");
//        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
//
//        long pending = 0L;
//        long overdue = 0L;
//
//        try {
//            pending = taskRepository.countPendingByEmployee(employeeId, completedNames);
//            overdue = taskRepository.countOverdueByEmployee(employeeId, completedNames, today);
//        } catch (Exception ex) {
//            log.warn("Employee-scoped optimized counts failed for {}, falling back to scan: {}", employeeId, ex.getMessage());
//            pending = taskRepository.findAll().stream()
//                    .filter(t -> {
//                        if (t.getAssignedEmployeeIds() == null) return false;
//                        if (!t.getAssignedEmployeeIds().contains(employeeId)) return false;
//                        boolean isCompleted = false;
//                        try {
//                            if (t.getTaskStage() != null && t.getTaskStage().getName() != null) {
//                                String n = t.getTaskStage().getName().toLowerCase();
//                                isCompleted = completedNames.contains(n);
//                            }
//                        } catch (Exception ignored) {}
//                        return !isCompleted;
//                    }).count();
//
//            overdue = taskRepository.findAll().stream()
//                    .filter(t -> {
//                        if (t.getAssignedEmployeeIds() == null) return false;
//                        if (!t.getAssignedEmployeeIds().contains(employeeId)) return false;
//                        boolean isCompleted = false;
//                        try {
//                            if (t.getTaskStage() != null && t.getTaskStage().getName() != null) {
//                                String n = t.getTaskStage().getName().toLowerCase();
//                                isCompleted = completedNames.contains(n);
//                            }
//                        } catch (Exception ignored) {}
//                        if (isCompleted) return false;
//                        if (Boolean.TRUE.equals(t.isNoDueDate())) return false;
//                        if (t.getDueDate() == null) return false;
//                        return t.getDueDate().isBefore(today);
//                    }).count();
//        }
//
//        return new TaskCountsDto(pending, overdue);
//    }
//
//    @Async
//    @Transactional(propagation = Propagation.NOT_SUPPORTED)
//    public void uploadFileInNewTransaction(Long taskId, MultipartFile file, String uploadedBy) {
//        try {
//            log.info("Starting async file upload for task: {}", taskId);
//            fileService.uploadTaskFile(taskId, file, uploadedBy);
//            log.info("Async file upload completed for task: {}", taskId);
//        } catch (Exception e) {
//            log.error("Async file upload failed for task {}: {}", taskId, e.getMessage());
//        }
//    }
//
//    // NEW: Helper to determine whether a stage name should be treated as Completed
//    private boolean isStageCompletedByName(String name) { // NEW
//        if (name == null) return false;
//        String n = name.trim().toLowerCase();
//        return n.equals("completed") || n.equals("complete") || n.equals("finished") || n.equals("finish");
//    }
//}
package com.erp.project_service.service.impl;

import com.erp.project_service.client.EmployeeClient;
import com.erp.project_service.dto.common.EmployeeMetaDto;
import com.erp.project_service.dto.file.FileMetaDto;
import com.erp.project_service.dto.milestone.MilestoneDto;
import com.erp.project_service.dto.task.*;
import com.erp.project_service.entity.*;
import com.erp.project_service.exception.BadRequestException;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.mapper.TaskMapper;
import com.erp.project_service.repository.*;
import com.erp.project_service.security.SecurityUtils;
import com.erp.project_service.service.TaskUserStateService;
import com.erp.project_service.service.interfaces.FileService;
import com.erp.project_service.service.interfaces.SubtaskService;
import com.erp.project_service.service.interfaces.TaskService;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import com.erp.project_service.service.notification.NotificationHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TaskServiceImpl - updated to use defensive/safe external calls so failures in employee-service
 * don't crash project-service. The change is mainly: use safeGetEmployeeMeta(...) wrapper
 * everywhere instead of calling employeeClient.getMeta(...) directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectActivityService activityService;
    private final ProjectRepository projectRepository;
    private final FileService fileService;
    private final FileMetaRepository fileMetaRepository;
    private final EmployeeClient employeeClient;
    private final LabelRepository labelRepository;
    private final ProjectMilestoneRepository milestoneRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final TaskStageRepository taskStageRepository;
    private final SubtaskService subtaskService;
    private final SubtaskRepository subtaskRepository;
    private final NotificationHelperService notificationHelper;
    private final TaskUserStateService taskUserStateService;
    private final TimeLogRepository timeLogRepository; // NEW: TimeLog repository dependency used for hours logged aggregation

    @Override
    @Transactional
    public TaskDto create(Long projectId, TaskCreateRequest req, String createdBy) {
        if (req == null) throw new BadRequestException("request body required");
        if (req.getProjectId() == null) throw new BadRequestException("projectId required in request");
        if (!projectId.equals(req.getProjectId())) throw new BadRequestException("projectId mismatch");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        Task t = TaskMapper.toEntity(req);
        t.setCreatedBy(createdBy);

        // Category handling (if provided)
        if (req.getCategoryId() != null && req.getCategoryId().getId() != null) {
            TaskCategory category = taskCategoryRepository.findById(req.getCategoryId().getId())
                    .orElseThrow(() -> new BadRequestException("Category not found: " + req.getCategoryId().getId()));
            t.setCategoryId(category);
        }

        if (t.getAssignedEmployeeIds() == null) t.setAssignedEmployeeIds(new HashSet<>());

        boolean isAdmin = SecurityUtils.isAdmin();

        TaskStage requestedTaskStage = null;
        if (req.getTaskStageId() != null) {
            requestedTaskStage = taskStageRepository.findById(req.getTaskStageId())
                    .orElseThrow(() -> new BadRequestException("TaskStage not found: " + req.getTaskStageId()));
        }

        if (isAdmin) {
            if (requestedTaskStage != null) {
                t.setTaskStage(requestedTaskStage);
                if (requestedTaskStage.getName() != null && !requestedTaskStage.getName().isBlank()) {
                    t.setStatusEnum(requestedTaskStage.getName());
                }
            } else {
                if (t.getStatusEnum() == null || t.getStatusEnum().isBlank()) {
                    t.setStatusEnum("INCOMPLETE");
                }
                if (t.getTaskStage() == null) {
                    TaskStage defaultStage = taskStageRepository.findFirstByOrderByIdAsc().orElse(null);
                    if (defaultStage != null) t.setTaskStage(defaultStage);
                }
            }
            t.setApprovedByAdmin(true);
            t.setApprovedAt(Instant.now());
            t.setApprovedBy(createdBy);

        } else {
            if (project.isTasksNeedAdminApproval()) {
                t.setStatusEnum("WAITING");
                TaskStage waitingStage = taskStageRepository.findByName("WAITING").orElse(null);
                if (waitingStage != null) t.setTaskStage(waitingStage);
                t.setApprovedByAdmin(false);
            } else {
                if (requestedTaskStage != null) {
                    t.setTaskStage(requestedTaskStage);
                    if (requestedTaskStage.getName() != null && !requestedTaskStage.getName().isBlank()) {
                        t.setStatusEnum(requestedTaskStage.getName());
                    }
                } else {
                    if (t.getStatusEnum() == null || t.getStatusEnum().isBlank()) {
                        t.setStatusEnum("INCOMPLETE");
                    }
                    if (t.getTaskStage() == null) {
                        TaskStage defaultStage = taskStageRepository.findFirstByOrderByIdAsc().orElse(null);
                        if (defaultStage != null) t.setTaskStage(defaultStage);
                    }
                }
                t.setApprovedByAdmin(true);
                t.setApprovedAt(Instant.now());
                t.setApprovedBy(createdBy);
            }
        }

        // ----- Additional validations -----
        if (t.getMilestoneId() != null) {
            ProjectMilestone milestone = milestoneRepository.findById(t.getMilestoneId())
                    .orElseThrow(() -> new BadRequestException("Milestone not found: " + t.getMilestoneId()));
            if (!projectId.equals(milestone.getProjectId())) {
                throw new BadRequestException("Milestone does not belong to project");
            }
        }

        if (req.getLabelIds() != null && !req.getLabelIds().isEmpty()) {
            List<Label> labels = labelRepository.findAllById(req.getLabelIds());
            List<Label> validLabels = labels.stream()
                    .filter(label -> label.getProjectId() != null && label.getProjectId().equals(projectId))
                    .collect(Collectors.toList());
            t.setLabels(new HashSet<>(validLabels));
        }

        if (Boolean.TRUE.equals(t.isDependent()) && t.getDependentTaskId() != null) {
            if (t.getDependentTaskId().equals(t.getId())) throw new BadRequestException("Task cannot depend on itself");
            Task dependent = taskRepository.findById(t.getDependentTaskId())
                    .orElseThrow(() -> new BadRequestException("Dependent task not found: " + t.getDependentTaskId()));
            if (!projectId.equals(dependent.getProjectId())) {
                throw new BadRequestException("Dependent task does not belong to same project");
            }
        }

        // only keep employees assigned to project
        if (t.getAssignedEmployeeIds() != null && !t.getAssignedEmployeeIds().isEmpty()) {
            List<String> notAssignedToProject = new ArrayList<>();
            List<String> validEmployees = new ArrayList<>();
            for (String empId : t.getAssignedEmployeeIds()) {
                if (project.getAssignedEmployeeIds() != null && project.getAssignedEmployeeIds().contains(empId)) {
                    validEmployees.add(empId);
                } else {
                    notAssignedToProject.add(empId);
                }
            }
            t.setAssignedEmployeeIds(new HashSet<>(validEmployees));
            if (!notAssignedToProject.isEmpty()) {
                log.warn("Employees {} are not assigned to project {}. They were not assigned to the task.",
                        notAssignedToProject, projectId);
            }
        }

        if (req.getTimeEstimate() == true){
            if (req.getTimeEstimateMinutes() != null) {
                t.setTimeEstimateMinutes(req.getTimeEstimateMinutes());
            }
        }

        // NEW: Set completedOn on create if the assigned TaskStage is a completed-like stage
        try {
            if (t.getTaskStage() != null && isStageCompletedByName(t.getTaskStage().getName())) {
                t.setCompletedOn(LocalDate.now(ZoneId.of("Asia/Kolkata")));
            } else {
                t.setCompletedOn(null);
            }
        } catch (Exception e) {
            log.warn("Failed to set completedOn during create: {}", e.getMessage());
        }

        // Persist
        Task saved = taskRepository.save(t);

        // File upload non-fatal
        if (req.getTaskFile() != null && !req.getTaskFile().isEmpty()) {
            try {
                uploadFileInNewTransaction(saved.getId(), req.getTaskFile(), createdBy);
            } catch (Exception e) {
                log.error("Failed to upload task file for task: " + saved.getId(), e.getMessage());
            }
        }

        // NEW: Send custom notifications to assigned employees
        if (req.getAssignedEmployeeIds() != null && !req.getAssignedEmployeeIds().isEmpty()) {
            sendTaskAssignmentNotifications(createdBy, req.getAssignedEmployeeIds(), req.getTitle(), saved.getId(), projectId, "CREATED");
        }

        activityService.record(projectId, createdBy, "TASK_CREATED", String.valueOf(saved.getId()));

        return enrichTaskDto(saved);
    }

    @Override
    @Transactional
    public TaskDto creates(Long projectId, EmployeeTaskCreateRequest req, String createdBy) {
        if (req == null) throw new BadRequestException("request body required");
        if (req.getProjectId() == null) throw new BadRequestException("projectId required in request");
        if (!projectId.equals(req.getProjectId())) throw new BadRequestException("projectId mismatch");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        Task t = TaskMapper.toEntitys(req);
        t.setCreatedBy(createdBy);

        // Category handling (if provided)
        if (req.getCategoryId() != null && req.getCategoryId().getId() != null) {
            TaskCategory category = taskCategoryRepository.findById(req.getCategoryId().getId())
                    .orElseThrow(() -> new BadRequestException("Category not found: " + req.getCategoryId().getId()));
            t.setCategoryId(category);
        }

        if (t.getAssignedEmployeeIds() == null) t.setAssignedEmployeeIds(new HashSet<>());

        boolean isAdmin = SecurityUtils.isAdmin();

        TaskStage requestedTaskStage = null;
        if (req.getTaskStageId() != null) {
            requestedTaskStage = taskStageRepository.findById(req.getTaskStageId())
                    .orElseThrow(() -> new BadRequestException("TaskStage not found: " + req.getTaskStageId()));
        }

        if (isAdmin) {
            if (requestedTaskStage != null) {
                t.setTaskStage(requestedTaskStage);
                if (requestedTaskStage.getName() != null && !requestedTaskStage.getName().isBlank()) {
                    t.setStatusEnum(requestedTaskStage.getName());
                }
            } else {
                if (t.getStatusEnum() == null || t.getStatusEnum().isBlank()) {
                    t.setStatusEnum("Incomplete");
                }
                if (t.getTaskStage() == null) {
                    TaskStage defaultStage = taskStageRepository.findFirstByOrderByIdAsc().orElse(null);
                    if (defaultStage != null) t.setTaskStage(defaultStage);
                }
            }
            t.setApprovedByAdmin(true);
            t.setApprovedAt(Instant.now());
            t.setApprovedBy(createdBy);

        } else {
            if (project.isTasksNeedAdminApproval()) {
                t.setStatusEnum("Waiting");
                TaskStage waitingStage = taskStageRepository.findByName("Waiting").orElse(null);
                if (waitingStage != null) t.setTaskStage(waitingStage);
                t.setApprovedByAdmin(false);
            } else {
                if (requestedTaskStage != null) {
                    t.setTaskStage(requestedTaskStage);
                    if (requestedTaskStage.getName() != null && !requestedTaskStage.getName().isBlank()) {
                        t.setStatusEnum(requestedTaskStage.getName());
                    }
                } else {
                    if (t.getStatusEnum() == null || t.getStatusEnum().isBlank()) {
                        t.setStatusEnum("Incomplete");
                    }
                    if (t.getTaskStage() == null) {
                        TaskStage defaultStage = taskStageRepository.findFirstByOrderByIdAsc().orElse(null);
                        if (defaultStage != null) t.setTaskStage(defaultStage);
                    }
                }
                t.setApprovedByAdmin(true);
                t.setApprovedAt(Instant.now());
                t.setApprovedBy(createdBy);
            }
        }

        // ----- Additional validations -----
        if (t.getMilestoneId() != null) {
            ProjectMilestone milestone = milestoneRepository.findById(t.getMilestoneId())
                    .orElseThrow(() -> new BadRequestException("Milestone not found: " + t.getMilestoneId()));
            if (!projectId.equals(milestone.getProjectId())) {
                throw new BadRequestException("Milestone does not belong to project");
            }
        }

        if (req.getLabelIds() != null && !req.getLabelIds().isEmpty()) {
            List<Label> labels = labelRepository.findAllById(req.getLabelIds());
            List<Label> validLabels = labels.stream()
                    .filter(label -> label.getProjectId() != null && label.getProjectId().equals(projectId))
                    .collect(Collectors.toList());
            t.setLabels(new HashSet<>(validLabels));
        }

        if (Boolean.TRUE.equals(t.isDependent()) && t.getDependentTaskId() != null) {
            if (t.getDependentTaskId().equals(t.getId())) throw new BadRequestException("Task cannot depend on itself");
            Task dependent = taskRepository.findById(t.getDependentTaskId())
                    .orElseThrow(() -> new BadRequestException("Dependent task not found: " + t.getDependentTaskId()));
            if (!projectId.equals(dependent.getProjectId())) {
                throw new BadRequestException("Dependent task does not belong to same project");
            }
        }

        // only keep employees assigned to project
        if (t.getAssignedEmployeeIds() != null && !t.getAssignedEmployeeIds().isEmpty()) {
            List<String> notAssignedToProject = new ArrayList<>();
            List<String> validEmployees = new ArrayList<>();
            for (String empId : t.getAssignedEmployeeIds()) {
                if (project.getAssignedEmployeeIds() != null && project.getAssignedEmployeeIds().contains(empId)) {
                    validEmployees.add(empId);
                } else {
                    notAssignedToProject.add(empId);
                }
            }
            t.setAssignedEmployeeIds(new HashSet<>(validEmployees));
            if (!notAssignedToProject.isEmpty()) {
                log.warn("Employees {} are not assigned to project {}. They were not assigned to the task.",
                        notAssignedToProject, projectId);
            }
        }

        if (req.getTimeEstimate() == true){
            if (req.getTimeEstimateMinutes() != null) {
                t.setTimeEstimateMinutes(req.getTimeEstimateMinutes());
            }
        }

        // NEW: Set completedOn on create(s) if the chosen stage is completed-like
        try {
            if (t.getTaskStage() != null && isStageCompletedByName(t.getTaskStage().getName())) {
                t.setCompletedOn(LocalDate.now(ZoneId.of("Asia/Kolkata")));
            } else {
                t.setCompletedOn(null);
            }
        } catch (Exception e) {
            log.warn("Failed to set completedOn during creates: {}", e.getMessage());
        }

        // Persist
        Task saved = taskRepository.save(t);

        // File upload non-fatal
        if (req.getTaskFile() != null && !req.getTaskFile().isEmpty()) {
            try {
                uploadFileInNewTransaction(saved.getId(), req.getTaskFile(), createdBy);
            } catch (Exception e) {
                log.error("Failed to upload task file for task: " + saved.getId(), e);
            }
        }

        activityService.record(projectId, createdBy, "TASK_CREATED", String.valueOf(saved.getId()));

        return enrichTaskDto(saved);
    }

    @Override
    public TaskDto get(Long projectId, Long taskId, String requesterId) {
        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        if (!t.getProjectId().equals(projectId)) throw new NotFoundException("Task not found in project");

        return enrichTaskDto(t);
    }

    @Override
    @Transactional
    public TaskDto update(Long projectId, Long taskId, TaskCreateRequest req, String updatedBy) {
        Task existingTask = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        if (!existingTask.getProjectId().equals(projectId)) throw new BadRequestException("project mismatch");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        // Apply updates (same as before)
        if (req.getTitle() != null) existingTask.setTitle(req.getTitle());
        if (req.getDescription() != null) existingTask.setDescription(req.getDescription());
        if (req.getStartDate() != null) existingTask.setStartDate(req.getStartDate());
        if (req.getDueDate() != null) existingTask.setDueDate(req.getDueDate());
        if (req.getNoDueDate() != null) existingTask.setNoDueDate(req.getNoDueDate());

        if (req.getTaskStageId() != null) {
            TaskStage taskStage = taskStageRepository.findById(req.getTaskStageId())
                    .orElseThrow(() -> new BadRequestException("TaskStage not found: " + req.getTaskStageId()));
            existingTask.setTaskStage(taskStage);
        }

        if (req.getMilestoneId() != null) existingTask.setMilestoneId(req.getMilestoneId());
        if (req.getPriority() != null) {
            try {
                existingTask.setPriority(TaskPriority.valueOf(req.getPriority()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid priority: " + req.getPriority());
            }
        }
        if (req.getIsPrivate() != null) existingTask.setPrivate(req.getIsPrivate());
        if (req.getTimeEstimate() != null) existingTask.setTimeEstimate(req.getTimeEstimate());
        if (req.getTimeEstimateMinutes() != null) existingTask.setTimeEstimateMinutes(req.getTimeEstimateMinutes());
        if (req.getIsDependent() != null) existingTask.setDependent(req.getIsDependent());
        if (req.getDependentTaskId() != null) existingTask.setDependentTaskId(req.getDependentTaskId());

        if (req.getCategoryId() != null) {
            if (req.getCategoryId().getId() != null) {
                TaskCategory category = taskCategoryRepository.findById(req.getCategoryId().getId())
                        .orElseThrow(() -> new BadRequestException("Category not found: " + req.getCategoryId().getId()));
                existingTask.setCategoryId(category);
                log.info("Updated category to ID: {} for task: {}", category.getId(), taskId);
            } else {
                existingTask.setCategoryId(null);
                log.info("Removed category from task: {}", taskId);
            }
        }

        if (req.getAssignedEmployeeIds() != null) {
            List<String> notAssignedToProject = new ArrayList<>();
            List<String> validEmployees = new ArrayList<>();

            for (String empId : req.getAssignedEmployeeIds()) {
                if (project.getAssignedEmployeeIds() != null && project.getAssignedEmployeeIds().contains(empId)) {
                    validEmployees.add(empId);
                } else {
                    notAssignedToProject.add(empId);
                }
            }

            existingTask.setAssignedEmployeeIds(new HashSet<>(validEmployees));

            if (!notAssignedToProject.isEmpty()) {
                log.warn("Employees {} are not assigned to project {}. They were not assigned to the task.",
                        notAssignedToProject, projectId);
            }
        }

        if (req.getLabelIds() != null) {
            try {
                if (req.getLabelIds().isEmpty()) {
                    existingTask.getLabels().clear();
                    log.info("Cleared all labels from task: {}", taskId);
                } else {
                    List<Label> labels = labelRepository.findAllById(req.getLabelIds());
                    List<Label> validLabels = labels.stream()
                            .filter(label -> label.getProjectId() != null && label.getProjectId().equals(projectId))
                            .collect(Collectors.toList());
                    existingTask.getLabels().clear();
                    existingTask.getLabels().addAll(validLabels);
                    log.info("Updated {} labels for task {}", validLabels.size(), taskId);
                }
            } catch (Exception e) {
                log.warn("Failed to update labels for task {}: {}", taskId, e.getMessage());
            }
        }

        existingTask.setUpdatedBy(updatedBy);

        // NEW/CHANGED: Update completedOn according to the new TaskStage if TaskStage was changed above
        try {
            if (existingTask.getTaskStage() != null) {
                if (isStageCompletedByName(existingTask.getTaskStage().getName())) {
                    existingTask.setCompletedOn(LocalDate.now(ZoneId.of("Asia/Kolkata")));
                } else {
                    existingTask.setCompletedOn(null);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to update completedOn during task update: {}", e.getMessage());
        }

        Task saved = taskRepository.save(existingTask);

        Task freshTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found after update"));

        if (freshTask.getLabels() != null) {
            freshTask.getLabels().size(); // Force initialization
        }

        if (req.getTaskFile() != null && !req.getTaskFile().isEmpty()) {
            try {
                fileService.uploadTaskFile(taskId, req.getTaskFile(), updatedBy);
            } catch (Exception e) {
                log.error("Failed to upload task file for task: " + taskId, e);
            }
        }

        if (req.getAssignedEmployeeIds() != null && !req.getAssignedEmployeeIds().isEmpty()) {
            sendTaskAssignmentNotifications(updatedBy, req.getAssignedEmployeeIds(), req.getTitle(), saved.getId(), projectId, "UPDATED");
        }

        activityService.record(projectId, updatedBy, "TASK_UPDATED", String.valueOf(saved.getId()));

        return enrichTaskDto(freshTask);
    }

    private TaskDto enrichTaskDto(Task task) {
        TaskDto dto = TaskMapper.toDto(task);

        // Enrich TaskStage data
        if (task.getTaskStage() != null) {
            dto.setTaskStageId(task.getTaskStage().getId());
        }

        // 1. Enrich assigned employees (safe)
        if (task.getAssignedEmployeeIds() != null && !task.getAssignedEmployeeIds().isEmpty()) {
            try {
                List<EmployeeMetaDto> assignedEmployees = new ArrayList<>();
                for (String empId : task.getAssignedEmployeeIds()) {
                    EmployeeMetaDto employeeMeta = safeGetEmployeeMeta(empId);
                    if (employeeMeta != null) {
                        assignedEmployees.add(employeeMeta);
                    }
                }
                dto.setAssignedEmployees(assignedEmployees);
            } catch (Exception e) {
                log.warn("Failed to enrich assigned employees: {}", e.getMessage());
            }
        }

        // 2. Enrich labels
        if (task.getLabels() != null) {
            try {
                log.debug("Task {} has {} labels", task.getId(), task.getLabels().size());
                List<LabelDto> labelDtos = task.getLabels().stream()
                        .map(label -> {
                            LabelDto labelDto = new LabelDto();
                            labelDto.setId(label.getId());
                            labelDto.setName(label.getName());
                            labelDto.setColorCode(label.getColorCode());
                            if (label.getProjectId() != null)
                            {
                                labelDto.setProjectId(label.getProjectId());
                                Project project = projectRepository.findById(label.getProjectId()).orElse(null);
                                labelDto.setProjectName(project.getName());
                                labelDto.setCreatedBy(label.getCreatedBy());
                            }
                            if (label.getDescription() != null) labelDto.setDescription(label.getDescription());
                            return labelDto;
                        })
                        .collect(Collectors.toList());
                dto.setLabels(labelDtos);
                log.debug("Successfully enriched {} labels for task {}", labelDtos.size(), task.getId());
            } catch (Exception e) {
                log.warn("Failed to enrich labels for task {}: {}", task.getId(), e.getMessage());
                dto.setLabels(new ArrayList<>());
            }
        } else {
            log.debug("Task {} has null labels collection", task.getId());
            dto.setLabels(new ArrayList<>());
        }

        // 3. Enrich milestone
        if (task.getMilestoneId() != null) {
            try {
                ProjectMilestone milestone = milestoneRepository.findById(task.getMilestoneId()).orElse(null);
                if (milestone != null) {
                    MilestoneDto milestoneDto = new MilestoneDto();
                    milestoneDto.setProjectId(milestone.getProjectId());
                    milestoneDto.setId(milestone.getId());
                    milestoneDto.setTitle(milestone.getTitle());
                    milestoneDto.setEndDate(milestone.getEndDate());
                    milestoneDto.setStartDate(milestone.getStartDate());
                    milestoneDto.setSummary(milestone.getSummary());
                    milestoneDto.setStatus(milestone.getStatus().toString());
                    milestoneDto.setMilestoneCost(milestone.getMilestoneCost());
                    milestoneDto.setCreatedBy(milestone.getCreatedBy());
                    milestoneDto.setUpdatedBy(milestone.getUpdatedBy());
                    milestoneDto.setCreatedAt(milestone.getCreatedAt());
                    milestoneDto.setUpdatedAt(milestone.getUpdatedAt());
                    dto.setMilestone(milestoneDto);
                }
            } catch (Exception e) {
                log.warn("Failed to enrich milestone: {}", e.getMessage());
            }
        }

        // 4. Enrich attachments
        try {
            List<FileMetaDto> attachments = fileService.listTaskFiles(task.getId());
            dto.setAttachments(attachments);
        } catch (Exception e) {
            log.warn("Failed to enrich attachments: {}", e.getMessage());
        }

        // Enrich project shortcode
        if (task.getProjectId() != null) {
            try {
                String projectShortCode = projectRepository.findShortCodeById(task.getProjectId());
                dto.setProjectShortCode(projectShortCode);
                dto.setProjectName(projectRepository.findById(task.getProjectId()).get().getName());

                log.debug("Successfully enriched project shortcode '{}' for task {}", projectShortCode, task.getId());
            } catch (Exception e) {
                log.warn("Failed to enrich project shortcode for projectId {}: {}", task.getProjectId(), e.getMessage());
                dto.setProjectShortCode(null);
            }
        }

        // Pinned state for current user
        try {
            String currentUser = SecurityUtils.getCurrentUserId();
            if (currentUser != null) {
                Instant pinnedAt = taskUserStateService.getPinnedAtForUser(task.getId(), currentUser);
                dto.setPinned(pinnedAt != null);
                dto.setPinnedAt(pinnedAt);
            } else {
                dto.setPinned(false);
                dto.setPinnedAt(null);
            }
        } catch (Exception e) {
            log.debug("Failed to set pinned state for task {}: {}", task.getId(), e.getMessage());
            dto.setPinned(false);
            dto.setPinnedAt(null);
        }

        // Hours logged aggregation
        try {
            Long minutes = null;
            try {
                minutes = timeLogRepository.sumDurationMinutesByTaskId(task.getId());
            } catch (Exception ex) {
                log.debug("Aggregation query failed for task {}: {}. Falling back to scanning timelogs.", task.getId(), ex.getMessage());
                minutes = 0L;
                try {
                    List<TimeLog> tlogs = timeLogRepository.findByTaskId(task.getId());
                    if (tlogs != null) {
                        for (TimeLog tl : tlogs) {
                            if (tl.getDurationMinutes() != null) minutes += tl.getDurationMinutes();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed fallback timelog fetch for task {}: {}", task.getId(), e.getMessage());
                    minutes = 0L;
                }
            }
            if (minutes == null) minutes = 0L;
            dto.setHoursLoggedMinutes(minutes);
            double hours = Math.round((minutes / 60.0) * 100.0) / 100.0; // 2 decimal rounding
            dto.setHoursLogged(hours);
        } catch (Exception e) {
            log.warn("Failed to compute hours logged for task {}: {}", task.getId(), e.getMessage());
            dto.setHoursLoggedMinutes(0L);
            dto.setHoursLogged(0.0);
        }

        // CompletedOn enrichment
        try {
            dto.setCompletedOn(task.getCompletedOn());
        } catch (Exception e) {
            dto.setCompletedOn(null);
        }

        return dto;
    }

    @Override
    @Transactional
    public void delete(Long projectId, Long taskId, String deletedBy) {
        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        if (!t.getProjectId().equals(projectId)) throw new NotFoundException("Task not found");

        List<Subtask> subtasks = subtaskRepository.findByTaskId(taskId);
        log.info("Deleting {} subtasks for task: {}", subtasks.size(), taskId);
        subtaskRepository.deleteAll(subtasks);

        List<FileMeta> taskFiles = fileMetaRepository.findByTaskId(taskId);
        System.out.println("Deleting " + taskFiles.size() + " files for project: " + taskId);

        for (FileMeta file : taskFiles) {
            try {
                fileService.deleteFile(file.getId(), deletedBy);
            } catch (Exception e) {
                System.err.println("Failed to delete file: " + file.getId() + ", error: " + e.getMessage());
            }
        }
        taskRepository.deleteById(taskId);
        activityService.record(projectId, deletedBy, "TASK_DELETED", String.valueOf(taskId));
    }

    @Override
    public List<TaskDto> listByProject(Long projectId) {
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        return tasks.stream()
                .map(this::enrichTaskDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDto> listAssignedTo(String employeeId) {
        return taskRepository.findByAssignedEmployeeId(employeeId, PageRequest.of(0, 1000))
                .stream()
                .map(this::enrichTaskDtos)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskDto changeStatus(Long projectId, Long taskId, Long taskStageId, String actor) {
        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        TaskStage taskStage = taskStageRepository.findById(taskStageId)
                .orElseThrow(() -> new BadRequestException("TaskStage not found: " + taskStageId));
        t.setTaskStage(taskStage);

        try {
            if (isStageCompletedByName(taskStage.getName())) {
                t.setCompletedOn(LocalDate.now(ZoneId.of("Asia/Kolkata")));
            } else {
                t.setCompletedOn(null);
            }
        } catch (Exception e) {
            log.warn("Failed to update completedOn during changeStatus: {}", e.getMessage());
        }

        Task saved = taskRepository.save(t);

        sendTaskStatusChangeNotification(actor, t.getAssignedEmployeeIds(), t.getTitle(), taskStage.getName(), taskId);

        activityService.record(projectId, actor, "TASK_STATUS_CHANGED", taskStageId.toString());
        return TaskMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TaskDto duplicate(Long projectId, Long taskId, String actor) {
        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        Task copy = Task.builder()
                .title(t.getTitle() + " (copy)")
                .categoryId(t.getCategoryId())
                .projectId(t.getProjectId())
                .startDate(t.getStartDate())
                .dueDate(t.getDueDate())
                .noDueDate(t.isNoDueDate())
                .taskStage(t.getTaskStage())
                .assignedEmployeeIds(t.getAssignedEmployeeIds())
                .description(t.getDescription())
                .milestoneId(t.getMilestoneId())
                .priority(t.getPriority())
                .isPrivate(t.isPrivate())
                .timeEstimateMinutes(t.getTimeEstimateMinutes())
                .isDependent(t.isDependent())
                .dependentTaskId(t.getDependentTaskId())
                .duplicateOfTaskId(t.getId())
                .createdBy(actor)
                .build();
        Task saved = taskRepository.save(copy);
        activityService.record(projectId, actor, "TASK_DUPLICATED", taskId + "->" + saved.getId());
        return TaskMapper.toDto(saved);
    }

    @Override
    @Transactional
    public TaskDto approveWaitingTask(Long projectId, Long taskId, String approver) {
        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        if (!"WAITING".equalsIgnoreCase(t.getStatusEnum())) throw new BadRequestException("Task not in waiting state");
        t.setStatusEnum("Incomplete");

        try {
            TaskStage incompleteStage = taskStageRepository.findByName("Incomplete")
                    .orElse(null);
            if (incompleteStage != null) {
                t.setTaskStage(incompleteStage);
            }
        } catch (Exception e) {
            log.warn("Failed to set INCOMPLETE task stage: {}", e.getMessage());
        }

        t.setCompletedOn(null);

        Task saved = taskRepository.save(t);

        sendTaskApprovalNotification(approver, t.getAssignedEmployeeIds(), t.getTitle(), taskId);

        activityService.record(projectId, approver, "TASK_APPROVED", String.valueOf(taskId));
        return TaskMapper.toDto(saved);
    }

    // Notification helpers (unchanged)
    private void sendTaskAssignmentNotifications(String actor, Set<String> assignedEmployeeIds, String taskTitle, Long taskId, Long projectId, String action) {
        try {
            String title = "";
            String message = "";

            if ("CREATED".equals(action)) {
                title = "🎯 New Task Assigned";
                message = String.format(
                        "You have been assigned a new task: '%s'. Task ID: %d | Project ID: %d. Please review the task details and start working on it.",
                        taskTitle, taskId, projectId
                );
            } else if ("UPDATED".equals(action)) {
                title = "📝 Task Updated";
                message = String.format(
                        "Task '%s' has been updated. Task ID: %d | Project ID: %d. Please check the updated details.",
                        taskTitle, taskId, projectId
                );
            }

            notificationHelper.sendBulkNotification(actor, new ArrayList<>(assignedEmployeeIds), title, message, "TASK_ASSIGNMENT");

            log.info("Task {} notifications sent to {} employees for task: {}", action, assignedEmployeeIds.size(), taskId);

        } catch (Exception e) {
            log.error("Failed to send task {} notifications: {}", action, e.getMessage());
        }
    }

    private void sendTaskStatusChangeNotification(String actor, Set<String> assignedEmployeeIds, String taskTitle, String newStatus, Long taskId) {
        try {
            String title = "🔄 Task Status Updated";
            String message = String.format(
                    "Task '%s' status has been changed to: %s. Task ID: %d.",
                    taskTitle, newStatus, taskId
            );

            notificationHelper.sendBulkNotification(actor, new ArrayList<>(assignedEmployeeIds), title, message, "TASK_STATUS_CHANGE");

            log.info("Task status change notification sent for task: {}", taskId);

        } catch (Exception e) {
            log.error("Failed to send task status change notification: {}", e.getMessage());
        }
    }

    private void sendTaskApprovalNotification(String approver, Set<String> assignedEmployeeIds, String taskTitle, Long taskId) {
        try {
            String title = "✅ Task Approved";
            String message = String.format(
                    "Your task '%s' has been approved by admin. Task ID: %d. You can now start working on this task.",
                    taskTitle, taskId
            );

            notificationHelper.sendBulkNotification(approver, new ArrayList<>(assignedEmployeeIds), title, message, "TASK_APPROVAL");

            log.info("Task approval notification sent for task: {}", taskId);

        } catch (Exception e) {
            log.error("Failed to send task approval notification: {}", e.getMessage());
        }
    }

    @Override
    public List<TaskDto> getAll() {
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream()
                .map(this::enrichTaskDtos)
                .collect(Collectors.toList());
    }

    private TaskDto enrichTaskDtos(Task task) {
        return enrichTaskDto(task);
    }

    @Override
    public void deleteTaskById(Long taskId, String deletedBy) {
        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

        List<FileMeta> taskFiles = fileMetaRepository.findByTaskId(taskId);
        System.out.println("Deleting " + taskFiles.size() + " files for task: " + taskId);

        for (FileMeta file : taskFiles) {
            try {
                fileService.deleteFile(file.getId(), deletedBy);
            } catch (Exception e) {
                System.err.println("Failed to delete file: " + file.getId() + ", error: " + e.getMessage());
            }
        }

        Long projectId = t.getProjectId();

        taskRepository.deleteById(taskId);
        activityService.record(projectId, deletedBy, "TASK_DELETED", String.valueOf(taskId));
    }

    @Override
    public List<TaskDto> getAllWaititngTask(String Status) {
        return taskRepository.findByStatusName(Status).stream().map(TaskMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeTaskCountDto getAssignedTaskCount(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return new EmployeeTaskCountDto(employeeId, 0L);
        }

        long count = 0L;

        try {
            try {
                java.lang.reflect.Method m = taskRepository.getClass().getMethod("countByAssignedEmployeeId", String.class);
                Object res = m.invoke(taskRepository, employeeId);
                if (res instanceof Number) {
                    count = ((Number) res).longValue();
                } else {
                    count = 0L;
                }
            } catch (NoSuchMethodException ignored) {
                org.springframework.data.domain.Page<Task> page =
                        taskRepository.findByAssignedEmployeeId(employeeId, org.springframework.data.domain.PageRequest.of(0, 1));
                count = page.getTotalElements();
            }
        } catch (Exception ex) {
            log.warn("Failed to compute task count via optimized path, falling back to scanning. Error: {}", ex.getMessage());
            count = taskRepository.findAll().stream()
                    .filter(t -> t.getAssignedEmployeeIds() != null && t.getAssignedEmployeeIds().contains(employeeId))
                    .count();
        }

        return new EmployeeTaskCountDto(employeeId, count);
    }

    @Transactional(readOnly = true)
    public TaskCountsDto getAllTasksCounts() {
        List<String> completedNames = Arrays.asList("completed", "finished", "complete", "finish");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        long pending = 0L;
        long overdue = 0L;

        try {
            pending = taskRepository.countPendingTasks(completedNames);
            overdue = taskRepository.countOverdueTasks(completedNames, today);
        } catch (Exception ex) {
            log.warn("Optimized task counts failed, falling back to in-memory scan: {}", ex.getMessage());
            pending = taskRepository.findAll().stream()
                    .filter(t -> {
                        boolean isCompleted = false;
                        try {
                            if (t.getTaskStage() != null && t.getTaskStage().getName() != null) {
                                String n = t.getTaskStage().getName().toLowerCase();
                                isCompleted = completedNames.contains(n);
                            }
                        } catch (Exception ignored) {}
                        return !isCompleted;
                    })
                    .count();

            overdue = taskRepository.findAll().stream()
                    .filter(t -> {
                        boolean isCompleted = false;
                        try {
                            if (t.getTaskStage() != null && t.getTaskStage().getName() != null) {
                                String n = t.getTaskStage().getName().toLowerCase();
                                isCompleted = completedNames.contains(n);
                            }
                        } catch (Exception ignored) {}

                        if (isCompleted) return false;
                        if (Boolean.TRUE.equals(t.isNoDueDate())) return false;
                        if (t.getDueDate() == null) return false;
                        return t.getDueDate().isBefore(today);
                    })
                    .count();
        }

        return new TaskCountsDto(pending, overdue);
    }

    @Transactional(readOnly = true)
    public TaskCountsDto getTasksCountsForEmployee(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) return new TaskCountsDto(0L, 0L);

        List<String> completedNames = Arrays.asList("completed", "finished", "complete", "finish");
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        long pending = 0L;
        long overdue = 0L;

        try {
            pending = taskRepository.countPendingByEmployee(employeeId, completedNames);
            overdue = taskRepository.countOverdueByEmployee(employeeId, completedNames, today);
        } catch (Exception ex) {
            log.warn("Employee-scoped optimized counts failed for {}, falling back to scan: {}", employeeId, ex.getMessage());
            pending = taskRepository.findAll().stream()
                    .filter(t -> {
                        if (t.getAssignedEmployeeIds() == null) return false;
                        if (!t.getAssignedEmployeeIds().contains(employeeId)) return false;
                        boolean isCompleted = false;
                        try {
                            if (t.getTaskStage() != null && t.getTaskStage().getName() != null) {
                                String n = t.getTaskStage().getName().toLowerCase();
                                isCompleted = completedNames.contains(n);
                            }
                        } catch (Exception ignored) {}
                        return !isCompleted;
                    }).count();

            overdue = taskRepository.findAll().stream()
                    .filter(t -> {
                        if (t.getAssignedEmployeeIds() == null) return false;
                        if (!t.getAssignedEmployeeIds().contains(employeeId)) return false;
                        boolean isCompleted = false;
                        try {
                            if (t.getTaskStage() != null && t.getTaskStage().getName() != null) {
                                String n = t.getTaskStage().getName().toLowerCase();
                                isCompleted = completedNames.contains(n);
                            }
                        } catch (Exception ignored) {}
                        if (isCompleted) return false;
                        if (Boolean.TRUE.equals(t.isNoDueDate())) return false;
                        if (t.getDueDate() == null) return false;
                        return t.getDueDate().isBefore(today);
                    }).count();
        }

        return new TaskCountsDto(pending, overdue);
    }

    @Override
    public TaskDto getByTaskId(Long taskId) {
        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        return enrichTaskDto(t);
    }

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void uploadFileInNewTransaction(Long taskId, MultipartFile file, String uploadedBy) {
        try {
            log.info("Starting async file upload for task: {}", taskId);
            fileService.uploadTaskFile(taskId, file, uploadedBy);
            log.info("Async file upload completed for task: {}", taskId);
        } catch (Exception e) {
            log.error("Async file upload failed for task {}: {}", taskId, e.getMessage());
        }
    }

    // NEW: Helper to determine whether a stage name should be treated as Completed
    private boolean isStageCompletedByName(String name) {
        if (name == null) return false;
        String n = name.trim().toLowerCase();
        return n.equals("completed") || n.equals("complete") || n.equals("finished") || n.equals("finish");
    }

    // SAFE wrapper for employeeClient.getMeta(...) so remote failures return null instead of throwing
    private EmployeeMetaDto safeGetEmployeeMeta(String employeeId) {
        if (employeeId == null) return null;
        try {
            return employeeClient.getMeta(employeeId);
        } catch (Exception ex) {
            log.debug("safeGetEmployeeMeta: failed for {} -> {}", employeeId, ex.getMessage());
            return null;
        }
    }
}
