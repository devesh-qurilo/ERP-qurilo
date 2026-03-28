package com.erp.project_service.controller;

import com.erp.project_service.entity.TaskUserState;
import com.erp.project_service.security.SecurityUtils;
import com.erp.project_service.service.TaskUserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@RestController
//@RequestMapping("/projects")
//@RequiredArgsConstructor
//public class TaskPinController {
//    private final TaskUserStateService service;
//
//    // Admin & Employee both can pin for themselves (per-user)
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
//    @PostMapping("/tasks/{taskId}/pin")
//    public ResponseEntity<Void> pin(@PathVariable Long taskId, Authentication auth) {
//        String actor = SecurityUtils.getCurrentUserId();
//        service.pin(taskId, actor, auth);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
//    @DeleteMapping("/tasks/{taskId}/pin")
//    public ResponseEntity<Void> unpin(@PathVariable Long taskId, Authentication auth) {
//        String actor = SecurityUtils.getCurrentUserId();
//        service.unpin(taskId, actor, auth);
//        return ResponseEntity.noContent().build();
//    }
//
//    // optional: My pinned tasks list (for pinned screen)
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
//    @GetMapping("/tasks/pinned")
//    public ResponseEntity<List<TaskUserState>> myPinned() {
//        String actor = SecurityUtils.getCurrentUserId();
//        return ResponseEntity.ok(service.listPinned(actor));
//    }
//}

// TaskPinController.java
//@RestController
//@RequestMapping("/projects")
//@RequiredArgsConstructor
//public class TaskPinController {
//    private final TaskUserStateService service;
//
//    // Admin & Employee both can pin for themselves (per-user)
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
//    @PostMapping("/tasks/{taskId}/pin")
//    public ResponseEntity<Void> pin(@PathVariable Long taskId) {
//        // let service determine current user + admin flag from SecurityUtils
//        service.pin(taskId);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
//    @DeleteMapping("/tasks/{taskId}/pin")
//    public ResponseEntity<Void> unpin(@PathVariable Long taskId) {
//        service.unpin(taskId);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
//    @GetMapping("/tasks/pinned")
//    public ResponseEntity<List<TaskUserState>> myPinned() {
//        String actor = SecurityUtils.getCurrentUserId();
//        return ResponseEntity.ok(service.listPinned(actor));
//    }
//}
//


import com.erp.project_service.entity.TaskUserState;
import com.erp.project_service.security.SecurityUtils;
import com.erp.project_service.service.TaskUserStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class TaskPinController {
    private final TaskUserStateService service;

    // Admin & Employee both can pin for themselves (per-user)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping("/tasks/{taskId}/pin")
    public ResponseEntity<Void> pin(@PathVariable Long taskId) {
        service.pin(taskId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @DeleteMapping("/tasks/{taskId}/pin")
    public ResponseEntity<Void> unpin(@PathVariable Long taskId) {
        service.unpin(taskId);
        return ResponseEntity.noContent().build();
    }

    // optional: My pinned tasks list (for pinned screen)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/tasks/pinned")
    public ResponseEntity<List<TaskUserState>> myPinned() {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.listPinned(actor));
    }

    /**
     * Admin endpoint: pin a task for some other user.
     * Example: POST /projects/tasks/17/pin-for?targetUserId=EMP-016
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping("/tasks/{taskId}/pin-for")
    public ResponseEntity<Void> pinFor(@PathVariable Long taskId,
                                       @RequestParam("targetUserId") String targetUserId) {
        // Only admin allowed to pin-for other users
        if (!SecurityUtils.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String actor = SecurityUtils.getCurrentUserId();
        service.pinForUser(taskId, targetUserId, actor);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin endpoint: unpin for another user
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @DeleteMapping("/tasks/{taskId}/pin-for")
    public ResponseEntity<Void> unpinFor(@PathVariable Long taskId,
                                         @RequestParam("targetUserId") String targetUserId) {
        if (!SecurityUtils.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String actor = SecurityUtils.getCurrentUserId();
        service.unpinForUser(taskId, targetUserId, actor);
        return ResponseEntity.noContent().build();
    }
}

