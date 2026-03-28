package com.erp.project_service.controller.admin;

import com.erp.project_service.dto.task.*;
import com.erp.project_service.mapper.TaskDtoEnricher;
import com.erp.project_service.service.interfaces.TaskCopyService;
import com.erp.project_service.service.interfaces.TaskService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/projects/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminTaskController {

    private final TaskService svc;
    private final TaskCopyService taskCopyService;
    private final TaskCreateRequestConverter requestConverter;
    private final TaskService taskService;
    private final TaskDtoEnricher taskEnricher; // 🔽 add


    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<TaskDto> create(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam("title") String title,
            @RequestParam(value = "category", required = false) Long categoryId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "dueDate", required = false) String dueDate,
            @RequestParam(value = "noDueDate", required = false) String noDueDate,
            @RequestParam(value = "taskStageId", required = false) Long taskStageId,
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
            @RequestParam(value = "dependentTaskId", required = false) String dependentTaskId) {

        String actor = SecurityUtils.getCurrentUserId();

        TaskCreateRequest req = requestConverter.fromFormData(
                title, categoryId, projectId.toString(), startDate, dueDate, noDueDate,
                taskStageId, taskFile, assignedEmployeeIds, description, labelIds,
                milestoneId, priority, isPrivate, timeEstimateMinutes, isDependent, dependentTaskId,timeEstimate
        );

        return ResponseEntity.status(201).body(svc.create(projectId, req, actor));
    }

    @PutMapping(value = "/{taskId}", consumes = "multipart/form-data")
    public ResponseEntity<TaskDto> update(
            @RequestParam(value = "projectId", required = false) Long projectId,
            @PathVariable Long taskId,
            @RequestParam("title") String title,
            @RequestParam(value = "category", required = false) Long categoryId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "dueDate", required = false) String dueDate,
            @RequestParam(value = "noDueDate", required = false) String noDueDate,
            @RequestParam(value = "taskStageId", required = false) Long taskStageId,
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
            @RequestParam(value = "dependentTaskId", required = false) String dependentTaskId) {

        String actor = SecurityUtils.getCurrentUserId();

        TaskCreateRequest req = requestConverter.fromFormData(
                title, categoryId, projectId.toString(), startDate, dueDate, noDueDate,
                taskStageId, taskFile, assignedEmployeeIds, description, labelIds,
                milestoneId, priority, isPrivate, timeEstimateMinutes, isDependent, dependentTaskId,timeEstimate
        );

        return ResponseEntity.ok(svc.update(projectId, taskId, req, actor));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> delete(@PathVariable Long projectId, @PathVariable Long taskId) {
        String actor = SecurityUtils.getCurrentUserId();
        svc.delete(projectId, taskId, actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> list(@PathVariable Long projectId) {
        String actor = SecurityUtils.getCurrentUserId();
        List<TaskDto> list =svc.listByProject(projectId);
        return ResponseEntity.ok(taskEnricher.enrichMany(list, actor));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> listTasksForEmployee(@PathVariable String employeeId) {
        try {
            List<TaskDto> tasks = taskService.listAssignedTo(employeeId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch tasks: " + e.getMessage());
        }
    }

    @GetMapping("/employee/{employeeId}/stats/count")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmployeeTaskCountDto> getAssignedTaskCount(@PathVariable String employeeId) {
        try {
            EmployeeTaskCountDto dto = taskService.getAssignedTaskCount(employeeId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new EmployeeTaskCountDto(employeeId, 0L));
        }
    }

    @GetMapping("/status/counts")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TaskCountsDto> getAllTasksCounts() {
        TaskCountsDto dto = taskService.getAllTasksCounts(); // or svc variant - use your injected service
        return ResponseEntity.ok(dto);
    }

}
