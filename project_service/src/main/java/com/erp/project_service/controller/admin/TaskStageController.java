package com.erp.project_service.controller.admin;

import com.erp.project_service.dto.task.TaskStageDto;
import com.erp.project_service.repository.TaskRepository;
import com.erp.project_service.service.interfaces.TaskStageService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/status")
@RequiredArgsConstructor
public class TaskStageController {

    private final TaskStageService svc;
    private final TaskRepository taskRepository;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<TaskStageDto> create(@RequestBody TaskStageDto dto) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(201).body(svc.create(dto, actor));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TaskStageDto> update(@PathVariable Long id, @RequestBody TaskStageDto dto) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.update(id, dto, actor));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        svc.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<TaskStageDto>> getAll() {
        return ResponseEntity.ok(svc.getAll());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskStageDto>> listForProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(svc.listForProject(projectId));
    }
}
