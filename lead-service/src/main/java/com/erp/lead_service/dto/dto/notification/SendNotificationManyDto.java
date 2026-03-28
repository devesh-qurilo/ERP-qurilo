package com.erp.lead_service.dto.dto.notification;

import lombok.Data;
import java.util.List;

@Data
public class SendNotificationManyDto {
    private List<String> receiverEmployeeIds; // multiple admins
    private String title;
    private String message;
    private String type;
}
