package com.erp.employee_service.controller;

import com.erp.employee_service.dto.pushNotification.RegisterPushTokenRequest;
import com.erp.employee_service.dto.pushNotification.UnregisterPushTokenRequest;
import com.erp.employee_service.service.pushNotification.PushService;
import com.erp.employee_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee/push")
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;
    private final SecurityUtils securityUtils;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterPushTokenRequest req) {
        String employeeId = securityUtils.getCurrentEmployeeId();
        if (req.getToken() == null || req.getToken().isBlank() || req.getProvider() == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "provider & token required"));
        }
        pushService.registerToken(employeeId, req.getProvider().toUpperCase(), req.getToken(), req.getDeviceInfo());
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }

    @PostMapping("/unregister")
    public ResponseEntity<?> unregister(@RequestBody UnregisterPushTokenRequest req) {
        if (req.getToken() == null || req.getToken().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "token required"));
        }
        pushService.unregisterToken(req.getToken());
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }
}
