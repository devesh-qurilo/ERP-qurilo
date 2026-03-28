package com.erp.employee_service.dto.pushNotification;

import lombok.Data;

@Data
public class RegisterPushTokenRequest {
    private String provider; // EXPO or FCM
    private String token;
    private String deviceInfo; // optional JSON/string
}
