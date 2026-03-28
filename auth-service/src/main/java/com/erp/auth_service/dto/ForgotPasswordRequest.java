package com.erp.auth_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {
    private String employeeId;
    private String email;
}
