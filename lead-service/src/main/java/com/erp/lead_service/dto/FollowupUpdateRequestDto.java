package com.erp.lead_service.dto;

import lombok.Data;
import java.time.LocalDate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Data
public class FollowupUpdateRequestDto {
    private LocalDate nextDate;

    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "startTime must be in HH:mm")
    private String startTime;

    private String remarks;
    private Boolean sendReminder;

    @Min(value = 0, message = "remindBefore must be non-negative")
    private Integer remindBefore;

    // "DAYS" / "HOURS" / "MINUTES"
    private String remindUnit;

    // NEW: status update
    private String status; // PENDING, CANCELLED, COMPLETED
}
