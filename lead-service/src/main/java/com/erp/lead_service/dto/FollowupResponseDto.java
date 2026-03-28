package com.erp.lead_service.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FollowupResponseDto {
    private Long id;
    private LocalDate nextDate;
    private String startTime;
    private String remarks;
    private Boolean sendReminder;
    private Boolean reminderScheduled;
    private Long dealId;
    private Integer remindBefore;
    private String remindUnit;
    private String status;
}
