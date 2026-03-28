package com.erp.auth_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private String employeeId;

    public LoginResponse(String accessToken,String refreshToken ,String role, String employeeId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
        this.employeeId = employeeId;
    }
}