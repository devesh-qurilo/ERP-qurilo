package com.erp.project_service.dto.timesheet;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TimeLogCreateRequest {
    private Long projectId;
    private Long taskId;
    @NotNull
    private String employeeId;
    @NotNull
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private String memo;
}
