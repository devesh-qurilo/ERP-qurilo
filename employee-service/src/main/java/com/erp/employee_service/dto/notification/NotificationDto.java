package com.erp.employee_service.dto.notification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDto {
    private Long id;
    private String senderEmployeeId;
    private String receiverEmployeeId;
    private String title;
    private String message;
    private String type;
    private boolean readFlag;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
