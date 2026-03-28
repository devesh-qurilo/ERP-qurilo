package com.erp.lead_service.dto.deal;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DealFollowupRequestDto {
    private LocalDate nextDate;
    private LocalTime startTime;
    private String remarks;
    private Boolean sendReminder = false;
}
