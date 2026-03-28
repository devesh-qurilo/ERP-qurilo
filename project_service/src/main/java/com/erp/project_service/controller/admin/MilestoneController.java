package com.erp.project_service.controller.admin;

import com.erp.project_service.dto.milestone.MilestoneCreateRequest;
import com.erp.project_service.dto.milestone.MilestoneDto;
import com.erp.project_service.service.interfaces.MilestoneService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService svc;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MilestoneDto> create(@PathVariable Long projectId, @RequestBody MilestoneCreateRequest req) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(201).body(svc.create(projectId, req, actor));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<List<MilestoneDto>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(svc.listByProjects(projectId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<MilestoneDto> get(@PathVariable Long projectId, @PathVariable Long id) {
        return ResponseEntity.ok(svc.get(projectId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MilestoneDto> update(@PathVariable Long projectId, @PathVariable Long id, @RequestBody MilestoneCreateRequest req) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.update(projectId, id, req, actor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long projectId, @PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        svc.delete(projectId, id, actor);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MilestoneDto> changeStatus(@PathVariable Long projectId, @PathVariable Long id, @RequestParam String status) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.changeStatus(projectId, id, status, actor));
    }
}
