package com.erp.auth_service.manualregister;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualRegisterRequest {
    private String employeeId;
    private String password;
    private String role;
    private String email;
}
