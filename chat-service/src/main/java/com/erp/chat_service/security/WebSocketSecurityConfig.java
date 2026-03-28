package com.erp.chat_service.security;

import com.erp.chat_service.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class WebSocketSecurityConfig {

    @Autowired
    private JwtUtil jwtUtil;

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public String getEmployeeIdFromToken(String token) {
        return jwtUtil.extractEmployeeId(token);
    }

    public Principal createPrincipal(String employeeId) {
        return new Principal() {
            @Override
            public String getName() {
                return employeeId; // Directly return String employeeId
            }
        };
    }
}