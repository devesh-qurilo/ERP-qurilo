package com.erp.employee_service.dto.attendance;

import lombok.Data;

import java.util.List;

/**
 * For marking an entire month for selected employees.
 * month: 1..12
 */
@Data
public class MonthAttendanceRequest {
    private int year;
    private int month;
    private List<String> employeeIds;
    private AttendancePayload payload;
    private boolean overwrite = false;
    private String markedBy;
}