package com.erp.project_service.controller;

import com.erp.project_service.entity.ProjectUserState;
import com.erp.project_service.security.SecurityUtils;
import com.erp.project_service.service.ProjectUserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectUserStateController {

    private final ProjectUserStateService service;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping("/{id}/pin")
    public ResponseEntity<Void> pin(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        service.pin(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @DeleteMapping("/{id}/pin")
    public ResponseEntity<Void> unpin(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        service.unpin(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        service.archive(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @DeleteMapping("/{id}/archive")
    public ResponseEntity<Void> unarchive(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        service.unarchive(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/pinned")
    public ResponseEntity<List<ProjectUserState>> myPinned() {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.listPinned(actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/archived")
    public ResponseEntity<List<ProjectUserState>> myArchived() {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.listArchived(actor));
    }
}
