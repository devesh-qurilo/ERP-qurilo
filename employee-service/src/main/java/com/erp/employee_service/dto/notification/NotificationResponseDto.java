package com.erp.employee_service.dto.notification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponseDto {
    private Long id;
    private String actorId;
    private String type;
    private String message;
    private String metadata;
    private boolean readFlag;
    private LocalDateTime createdAt;
}
