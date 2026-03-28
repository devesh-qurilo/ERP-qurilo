package com.erp.employee_service.dto.notification;

import lombok.Data;

@Data
public class SendNotificationDto {
    private String receiverEmployeeId; // single receiver
    private String title;
    private String message;
    private String type;
}
