package com.erp.project_service.controller;

import com.erp.project_service.dto.task.*;
import com.erp.project_service.mapper.TaskDtoEnricher;
import com.erp.project_service.repository.ProjectRepository;
import com.erp.project_service.service.interfaces.TaskService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService svc;
    private final ProjectRepository repository;
    private final TaskEmployeeCreateRequestConverter requestConverter;
    private final TaskDtoEnricher taskEnricher; // 🔽 add


    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE')")
    @PostMapping("/tasks")
    public ResponseEntity<TaskDto> create(
                                          @RequestParam(value = "projectId", required = false)  Long projectId,
                                          @RequestParam("title") String title,
                                          @RequestParam(value = "category", required = false) Long categoryId,
                                          @RequestParam(value = "startDate", required = false) String startDate,
                                          @RequestParam(value = "dueDate", required = false) String dueDate,
                                          @RequestParam(value = "noDueDate", required = false) String noDueDate,
                                          @RequestParam(value = "taskFile", required = false) MultipartFile taskFile,
                                          @RequestParam(value = "assignedEmployeeIds", required = false) String assignedEmployeeIds,
                                          @RequestParam(value = "description", required = false) String description,
                                          @RequestParam(value = "labelIds", required = false) String labelIds,
                                          @RequestParam(value = "milestoneId", required = false) String milestoneId,
                                          @RequestParam(value = "priority", required = false) String priority,
                                          @RequestParam(value = "isPrivate", required = false) String isPrivate,
                                          @RequestParam(value = "timeEstimate", required = false)  String timeEstimate,
                                          @RequestParam(value = "timeEstimateMinutes", required = false) String timeEstimateMinutes,
                                          @RequestParam(value = "isDependent", required = false) String isDependent,
                                          @RequestParam(value = "dependentTaskId", required = false) Long dependentTaskId) {

        // both admin & employees allowed to create; service enforces waiting state if needed
        String actor = SecurityUtils.getCurrentUserId();

        boolean isAssigned = repository.isEmployeeAssignedToProject(projectId, actor);
        if (!isAssigned) {
            return ResponseEntity.badRequest().build();
        }

        EmployeeTaskCreateRequest req = requestConverter.fromFormData(
                title, categoryId, projectId.toString(), startDate, dueDate, noDueDate,
                taskFile, assignedEmployeeIds, description, labelIds,
                milestoneId, priority, isPrivate, timeEstimateMinutes, timeEstimate, isDependent, dependentTaskId
        );

        return ResponseEntity.status(201).body(svc.creates(projectId, req, actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("{projectId}/tasks/{taskId}")
    public ResponseEntity<TaskDto> get(@PathVariable Long projectId, @PathVariable Long taskId) {
        String actor = SecurityUtils.getCurrentUserId();
        boolean isAssigned = repository.isEmployeeAssignedToProject(projectId, actor);
        if (!isAssigned) {
            return ResponseEntity.badRequest().build();
        }
        TaskDto dto = svc.get(projectId, taskId, actor);
        return ResponseEntity.ok(taskEnricher.enrichOne(dto, actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long taskId) {
        TaskDto dto = svc.getByTaskId(taskId);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("{projectId}/tasks")
    public ResponseEntity<List<TaskDto>> list(@PathVariable Long projectId) {
        String actor = SecurityUtils.getCurrentUserId();
        boolean isAssigned = repository.isEmployeeAssignedToProject(projectId, actor);
        if (!isAssigned) {
            return ResponseEntity.badRequest().build();
        }
        List<TaskDto> list= svc.listByProject(projectId);
        return ResponseEntity.ok(taskEnricher.enrichMany(list, actor));
    }


    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/tasks/counts/me")
    public ResponseEntity<TaskCountsDto> getMyTaskCounts() {
        String me = SecurityUtils.getCurrentUserId();
        TaskCountsDto dto = svc.getTasksCountsForEmployee(me);
        return ResponseEntity.ok(dto);
    }
}
