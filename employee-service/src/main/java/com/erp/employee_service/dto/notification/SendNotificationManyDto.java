package com.erp.employee_service.dto.notification;

import lombok.Data;

import java.util.List;

@Data
public class SendNotificationManyDto {
    private List<String> receiverEmployeeIds;
    private String title;
    private String message;
    private String type;
}
