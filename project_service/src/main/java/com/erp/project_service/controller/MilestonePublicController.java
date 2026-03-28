package com.erp.project_service.controller;

import com.erp.project_service.dto.milestone.MilestoneDto;
import com.erp.project_service.security.SecurityUtils;
import com.erp.project_service.service.interfaces.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/milestones")
@RequiredArgsConstructor
public class MilestonePublicController {

    private final MilestoneService svc;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<MilestoneDto>> list(@PathVariable Long projectId) {
        String requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.listByProject(projectId, requesterId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<MilestoneDto> get(@PathVariable Long projectId, @PathVariable Long id) {
        return ResponseEntity.ok(svc.get(projectId, id));
    }
}
