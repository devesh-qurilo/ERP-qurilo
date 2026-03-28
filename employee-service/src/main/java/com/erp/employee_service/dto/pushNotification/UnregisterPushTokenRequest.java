package com.erp.employee_service.dto.pushNotification;
import lombok.Data;

@Data
public class UnregisterPushTokenRequest {
    private String token;
}
