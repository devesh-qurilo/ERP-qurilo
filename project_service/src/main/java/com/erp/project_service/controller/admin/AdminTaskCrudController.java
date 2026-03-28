package com.erp.project_service.controller.admin;

import com.erp.project_service.dto.task.TaskDto;
import com.erp.project_service.entity.Task;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.repository.TaskRepository;
import com.erp.project_service.security.SecurityUtils;
import com.erp.project_service.service.interfaces.TaskCopyService;
import com.erp.project_service.service.interfaces.TaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminTaskCrudController {
    private final TaskService svc;
    private final TaskRepository taskRepository;
    private final TaskCopyService copyService;

    @GetMapping("/getAll")
    public ResponseEntity<List<TaskDto>> getAll() {
        return ResponseEntity.ok(svc.getAll());
    }

    @DeleteMapping("/{taskId}/delete")
    public ResponseEntity<?> delete(@PathVariable Long taskId) {
        String actor = SecurityUtils.getCurrentUserId();
        svc.deleteTaskById(taskId,actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/waiting")
    public ResponseEntity<List<TaskDto>> getAllWaitingTask() {
        String Status = "waiting";
        return ResponseEntity.ok(svc.getAllWaititngTask(Status));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskDto> changeStatus(@PathVariable Long taskId, @RequestParam Long statusId) {
        String actor = SecurityUtils.getCurrentUserId();
        Task t = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        Long projectId = t.getProjectId(); // use the already loaded Task
        return ResponseEntity.ok(svc.changeStatus(projectId, taskId, statusId, actor));
    }

    @PostMapping("/{taskId}/duplicate")
    public ResponseEntity<TaskDto> duplicate(@PathVariable Long taskId) {
        String actor = SecurityUtils.getCurrentUserId();
        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        Long projectId = taskRepository.getProjectById(t.getProjectId());
        return ResponseEntity.ok(svc.duplicate(projectId, taskId, actor));
    }

    @PostMapping("/{taskId}/approve")
    public ResponseEntity<TaskDto> approve(@PathVariable Long taskId) {
        String actor = SecurityUtils.getCurrentUserId();
        Task t = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        Long projectId = taskRepository.getProjectById(t.getProjectId());
        return ResponseEntity.ok(svc.approveWaitingTask(projectId, taskId, actor));
    }

    @PostMapping("/{taskId}/copy-links")
    public ResponseEntity<java.util.Map<String,String>> createCopyLink(@PathVariable Long taskId) throws JsonProcessingException {
        // fetch task, build TaskDto via TaskMapper + enrich as needed
        Task t1 = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        Long projectId = taskRepository.getProjectById(t1.getProjectId());
        TaskDto t = svc.get(projectId, taskId, SecurityUtils.getCurrentUserId());
        String snapshot = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(t);
        UUID key = copyService.createCopy(projectId, taskId, snapshot, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(Map.of("link", "/task-copy/" + key.toString()));
    }
}
