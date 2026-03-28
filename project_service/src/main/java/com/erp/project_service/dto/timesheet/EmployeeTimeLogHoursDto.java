package com.erp.project_service.dto.timesheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTimeLogHoursDto {
    private String employeeId;
    // total minutes across all timelogs (for precision)
    private Long totalMinutes;
    // total hours (minutes / 60) with 2 decimal places
    private BigDecimal totalHours;
}
