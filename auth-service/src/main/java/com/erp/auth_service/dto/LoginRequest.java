package com.erp.auth_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String employeeId;
    private String password;
}
