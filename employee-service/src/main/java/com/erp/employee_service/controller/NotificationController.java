package com.erp.employee_service.controller;

import com.erp.employee_service.dto.notification.*;
import com.erp.employee_service.service.notification.NotificationService;
import com.erp.employee_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService svc;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDto>> myNotifications(java.security.Principal p) {
        return ResponseEntity.ok(svc.getMyNotifications(p.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationDto> getById(@PathVariable Long id, java.security.Principal p) {
        return ResponseEntity.ok(svc.getById(id));
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<Void> send(@RequestBody SendNotificationDto dto, java.security.Principal p) {
        svc.sendNotification(p != null ? p.getName() : null, dto);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/send-many")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<Void> sendMany(@RequestBody SendNotificationManyDto dto, java.security.Principal p) {
        svc.sendNotificationMany(p != null ? p.getName() : null, dto.getReceiverEmployeeIds(), dto.getTitle(), dto.getMessage(), dto.getType());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/send-many")
    public ResponseEntity<Void> sendManyInternal(@RequestBody SendNotificationManyDto dto) {
        // Use SecurityUtils to get employee ID from authentication
        String employeeId = securityUtils.getCurrentEmployeeId();
        svc.sendNotificationMany(employeeId, dto.getReceiverEmployeeIds(), dto.getTitle(), dto.getMessage(), dto.getType());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/internal/send")
    public ResponseEntity<Void> sends(@RequestBody SendNotificationDto dto) {
        String employeeId = securityUtils.getCurrentEmployeeId();
        svc.sendNotification(employeeId, dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> markRead(@PathVariable Long id, java.security.Principal p) {
        svc.markRead(id, p.getName());
        return ResponseEntity.ok("success");
    }

    @PostMapping("/{id}/mark-unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markUnread(@PathVariable Long id, java.security.Principal p) {
        svc.markUnread(id, p.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/clear")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearAll(java.security.Principal p) {
        svc.clearAll(p.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable Long id, java.security.Principal p) {
        // This endpoint only allows a receiver to delete own notifications.
        svc.deleteById(id, p.getName());
        return ResponseEntity.ok().build();
    }

    // Admin: delete any notification
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminDelete(@PathVariable Long id) {
        // direct repository delete could be exposed in service too; keep simple call to repo inside a new admin method if needed
        // assume NotificationServiceImpl has an admin delete method or repository is injected into controller (not ideal)
        throw new UnsupportedOperationException("Use service admin delete if implemented");
    }

}
