package com.erp.project_service.dto.timesheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeLogDayResponse {
    // full timelog entries for that day (DTO already exists: TimeLogDto)
    private java.util.List<com.erp.project_service.dto.timesheet.TimeLogDto> timeLogs;
    // summary to render progress bar and duration label
    private TimeLogDaySummaryDto summary;
}
