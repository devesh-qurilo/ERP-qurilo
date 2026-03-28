package com.erp.employee_service.dto.notification;

import lombok.Data;
import java.util.List;

@Data
public class SendNotificationRequestDto {
    private String senderId;           // optional override, use auth by default
    private List<String> receiverIds;  // one or many employeeIds
    private String title;
    private String message;
    private String type;               // e.g. "LEAVE","PROMOTION","SYSTEM"
}
