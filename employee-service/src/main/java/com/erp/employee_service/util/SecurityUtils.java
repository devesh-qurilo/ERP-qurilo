package com.erp.employee_service.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public String getCurrentEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String employeeId = authentication.getName();

            // If no employee ID found, return a default system user
            if (employeeId == null || employeeId.equals("anonymousUser")) {
                return "EMP-009"; // Your system user
            }
            return employeeId;
        }
        return "EMP-009"; // Fallback to system user
    }

    public String getCurrentUsername() {
        return getCurrentEmployeeId();
    }
}