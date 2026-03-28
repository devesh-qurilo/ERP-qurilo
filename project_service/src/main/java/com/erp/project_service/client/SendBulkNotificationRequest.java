package com.erp.project_service.client;

import lombok.Data;

import java.util.List;

@Data
public class SendBulkNotificationRequest {
    private List<String> receiverEmployeeIds;
    private String title;
    private String message;
    private String type;
}