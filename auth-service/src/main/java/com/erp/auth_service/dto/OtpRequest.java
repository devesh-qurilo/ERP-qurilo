package com.erp.auth_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {
    private String employeeId;
    private String email;
}
