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
public class TimeLogSegmentDto {
    private Long projectId;
    private String projectName; // optional, may be null
    private String projectShortCode;
    private Long minutes;       // minutes spent for this project on that day
    private BigDecimal hours;   // minutes/60 scaled to 2 decimals
    private String color;       // optional color (if you have project color)
}
