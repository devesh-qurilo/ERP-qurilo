package com.erp.auth_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerifyRequest {
    private String employeeId;
    private String otp;
}
