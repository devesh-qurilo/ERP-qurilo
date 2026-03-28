package com.erp.auth_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    private String employeeId;
    private String newPassword;
}
