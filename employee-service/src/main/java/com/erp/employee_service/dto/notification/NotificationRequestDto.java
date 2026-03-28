package com.erp.employee_service.dto.notification;

import lombok.Data;

import java.util.List;

@Data
public class NotificationRequestDto {
    private List<String> targetEmployeeIds;
    private String type;
    private String message;
    private String metadata;
}
