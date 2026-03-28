package com.erp.project_service.controller;

import com.erp.project_service.dto.activity.ProjectActivityDto;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/activity")
@RequiredArgsConstructor
public class ProjectActivityController {

    private final ProjectActivityService svc;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<ProjectActivityDto>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(svc.listForProject(projectId));
    }
}
