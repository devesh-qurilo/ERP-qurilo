package com.erp.project_service.controller;

import com.erp.project_service.service.interfaces.TaskReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskReminderController {

    private final TaskReminderService taskReminderService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{taskId}/reminder")
    public ResponseEntity<String> sendReminder(
            @PathVariable Long taskId,
            @AuthenticationPrincipal String currentUser) {

        log.info("Sending reminder for task: {} by user: {}", taskId, currentUser);

        taskReminderService.sendReminder(taskId, currentUser);

        return ResponseEntity.ok("Reminder sent successfully to all assigned employees");
    }
}