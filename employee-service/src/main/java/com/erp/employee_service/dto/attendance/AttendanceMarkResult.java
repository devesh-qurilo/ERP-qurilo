package com.erp.employee_service.dto.attendance;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceMarkResult {
    private LocalDate date;
    private String employeeId;
    private String employeeName;
    private String status; // PRESENT | LEAVE | HOLIDAY | ABSENT (status for compatibility)

    // attendance details (if present)
    private Long attendanceId;
    private Boolean overwritten;
    private Boolean late;
    private Boolean halfDay;

    private LocalTime clockInTime;
    private String clockInLocation;
    private String clockInWorkingFrom;

    private LocalTime clockOutTime;
    private String clockOutLocation;
    private String clockOutWorkingFrom;

    // marked by info (optional)
    private String markedById;
    private String markedByName;

    // New flags requested
    private Boolean holiday = Boolean.FALSE;
    private Boolean leave = Boolean.FALSE;

    /**
     * Final computed isPresent value returned to client:
     * - true if attendance exists and not (leave/holiday) OR overwritten == true
     * - false if leave/holiday exists and not overwritten
     * - if no attendance record, null (or false) — here we'll set false
     */
    private Boolean isPresent = Boolean.FALSE;
}