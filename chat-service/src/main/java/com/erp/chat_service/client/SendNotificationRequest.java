package com.erp.chat_service.client;

import lombok.Data;

@Data
public class SendNotificationRequest {
    private String receiverEmployeeId;
    private String title;
    private String message;
    private String type = "Chat";
}
