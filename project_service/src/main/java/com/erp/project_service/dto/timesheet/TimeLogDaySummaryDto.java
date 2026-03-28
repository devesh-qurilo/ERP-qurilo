package com.erp.project_service.dto.timesheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeLogDaySummaryDto {
    private LocalDate date;
    private Long totalMinutes;
    private BigDecimal totalHours;
    private List<TimeLogSegmentDto> segments; // breakdown per project
}
