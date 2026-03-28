package com.erp.lead_service.dto.dto.notification;

import lombok.Data;

@Data
public class SendNotificationDto {
    private String receiverEmployeeId; // किसे भेजना है
    private String title;              // टाइटल
    private String message;            // मैसेज
    private String type;               // e.g. FOLLOWUP_REMINDER
}
