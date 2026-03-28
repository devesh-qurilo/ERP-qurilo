package com.erp.chat_service.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationManyRequest {
    private List<String> receiverEmployeeIds;
    private String title;
    private String message;
    private String type = "CHAT";
}