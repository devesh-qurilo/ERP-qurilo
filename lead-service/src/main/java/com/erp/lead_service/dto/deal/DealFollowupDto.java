package com.erp.lead_service.dto.deal;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class DealFollowupDto {
    private Long id;
    private LocalDate nextDate;
    private LocalTime startTime;
    private String remarks;
    private Boolean sendReminder;
    private Boolean reminderSent;
    private LocalDateTime createdAt;
}
