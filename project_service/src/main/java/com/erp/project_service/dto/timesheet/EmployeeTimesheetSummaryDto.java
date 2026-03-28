package com.erp.project_service.dto.timesheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTimesheetSummaryDto {
    private String employeeId;
    private String employeeName;
    private String employeeEmail;
    private String designation;
    private Long totalMinutes;
    private BigDecimal totalHours;
    private List<TimeLogDto> timeLogs;
}

