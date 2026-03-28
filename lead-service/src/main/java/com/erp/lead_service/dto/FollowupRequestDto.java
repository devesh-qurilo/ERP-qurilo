package com.erp.lead_service.dto;

import lombok.Data;
import java.time.LocalDate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Data
public class FollowupRequestDto {
    private LocalDate nextDate;

    // HH:mm format recommended
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "startTime must be in HH:mm")
    private String startTime; // HH:mm

    private String remarks;

    // whether to send reminder
    private Boolean sendReminder;

    // Only meaningful if sendReminder == true:
    @Min(value = 0, message = "remindBefore must be non-negative")
    private Integer remindBefore; // e.g., 1

    // "DAYS", "HOURS", or "MINUTES"
    private String remindUnit;
}
