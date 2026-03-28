package com.erp.project_service.controller;

import com.erp.project_service.dto.task.SubtaskCreateRequest;
import com.erp.project_service.dto.task.SubtaskDto;
import com.erp.project_service.entity.Subtask;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.repository.SubtaskRepository;
import com.erp.project_service.service.interfaces.SubtaskService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks/{taskId}/subtasks")
@RequiredArgsConstructor
public class SubtaskController {

    private final SubtaskService svc;
    private final SubtaskRepository subtaskRepository;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping
    public ResponseEntity<SubtaskDto> create(@PathVariable Long taskId, @RequestBody SubtaskCreateRequest req) {
        // ensure taskId consistency; service does validation
        String actor = SecurityUtils.getCurrentUserId();
        req.setTaskId(taskId);
        return ResponseEntity.status(201).body(svc.create(req, actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<SubtaskDto>> list(@PathVariable Long taskId) {
        return ResponseEntity.ok(svc.listByTask(taskId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{subtaskId}")
    public ResponseEntity<SubtaskDto> update(@PathVariable Long subtaskId, @RequestBody SubtaskCreateRequest req) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.update(subtaskId, req, actor));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{subtaskId}")
    public ResponseEntity<?> delete(@PathVariable Long taskId, @PathVariable Long subtaskId) {
        String actor = SecurityUtils.getCurrentUserId();

        // Find subtask and validate ownership
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new NotFoundException("Subtask not found"));

        if (!subtask.getTaskId().equals(taskId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Subtask does not belong to the specified task");
        }

        svc.delete(subtaskId, actor);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{subtaskId}")
    public ResponseEntity<String> toggleStatus(@PathVariable Long subtaskId){
        String actor = SecurityUtils.getCurrentUserId();
        svc.toggleStatus(subtaskId, actor);
        return ResponseEntity.status(200).body("Subtask toggled Successfully");
    }
}
