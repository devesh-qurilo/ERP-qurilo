package com.erp.project_service.dto.timesheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyTimeLogCreateResponse {

    private String employeeId;
    private Long projectId;
    private Long taskId;

    /**
     * Jo naye TimeLog create hue, unka detail
     */
    private List<TimeLogDto> createdLogs;

    /**
     * Jis date pr already timelog mila (skip kiya)
     */
    private List<LocalDate> alreadyFilledDates;

    /**
     * Jaha hours null/0 the, ya date invalid thi
     */
    private List<LocalDate> skippedInvalidDates;
}
