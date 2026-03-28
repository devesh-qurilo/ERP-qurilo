package com.erp.project_service.dto.timesheet;

import com.erp.project_service.dto.common.EmployeeMetaDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class TimeLogDto {
    private Long id;
    private Long projectId;
    private String projectShortCode; // ✅ NEW FIELD
    private  String projectName;
    private Long taskId;
    private String taskName;
    private String employeeId;
    private List<EmployeeMetaDto> employees;
    private LocalDate startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime startTime;
    private LocalDate endDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime endTime;
    private String memo;
    private Long durationHours;
    private String createdBy;
    private Instant createdAt;
}
