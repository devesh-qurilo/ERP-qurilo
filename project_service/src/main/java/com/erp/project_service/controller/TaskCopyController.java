package com.erp.project_service.controller;

import com.erp.project_service.dto.task.TaskDto;
import com.erp.project_service.service.interfaces.TaskCopyService;
import com.erp.project_service.service.interfaces.TaskService;
import com.erp.project_service.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Handles task copy-link creation (admin) and public snapshot retrieval.
 *
 * Endpoints:
 *  - POST  /admin/projects/{projectId}/tasks/{taskId}/copy-link    (ROLE_ADMIN)
 *  - GET   /task-copy/{id}                                        (public read-only)
 */
@RestController
@RequiredArgsConstructor
public class TaskCopyController {

    private final TaskService taskService;
    private final TaskCopyService taskCopyService;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/projects/{projectId}/tasks/{taskId}/copy-link")
    public ResponseEntity<Map<String, String>> createCopyLink(
            @PathVariable Long projectId,
            @PathVariable Long taskId
    ) throws Exception {
        // Ensure admin identity present
        String actor = SecurityUtils.getCurrentUserId();

        // Validate and fetch task DTO (service will check project/task relationship)
        TaskDto t = taskService.get(projectId, taskId, actor);

        // Create JSON snapshot
        String snapshot = objectMapper.writeValueAsString(t);

        // Persist snapshot and get UUID
        java.util.UUID key = taskCopyService.createCopy(projectId, taskId, snapshot, actor);

        String linkPath = "/task-copy/" + key.toString();
        return ResponseEntity.ok(Map.of("link", linkPath, "id", key.toString()));
    }

    // Public read-only endpoint to fetch the snapshot JSON
    @GetMapping("/task-copy/{id}")
    public ResponseEntity<String> getCopy(@PathVariable("id") UUID id) {
        String snapshot = taskCopyService.getSnapshot(id);
        if (snapshot == null) return ResponseEntity.notFound().build();
        // Return raw JSON snapshot. Client can parse it.
        return ResponseEntity.ok(snapshot);
    }
}
