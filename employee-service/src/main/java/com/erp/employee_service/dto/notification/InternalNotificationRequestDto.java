package com.erp.employee_service.dto.notification;

import lombok.Data;

import java.util.List;

@Data
public class InternalNotificationRequestDto {
    private String actorId;            // who triggered the action (service name or user id)
    private List<String> targetEmployeeIds; // list of employeeId strings (employee.employeeId)
    private String type;               // PROMOTION, CUSTOM, ...
    private String message;            // human readable message
    private String metadata;           // optional JSON string
}
