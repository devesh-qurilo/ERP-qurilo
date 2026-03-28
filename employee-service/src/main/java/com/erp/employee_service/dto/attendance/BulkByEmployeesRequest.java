package com.erp.employee_service.dto.attendance;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Alias request (same as BulkAttendanceRequest) - kept separate for clarity
 * when UI sends employees by department/filter.
 */
@Data
public class BulkByEmployeesRequest {
    private List<String> employeeIds;
    private List<LocalDate> dates;
    private AttendancePayload payload;
    private boolean overwrite = false;
    private String markedBy;
}