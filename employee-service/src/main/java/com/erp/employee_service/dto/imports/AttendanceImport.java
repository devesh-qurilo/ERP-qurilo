package com.erp.employee_service.dto.imports;

import lombok.Data;

/**
 * Fields kept as String so parser can flexibly accept many formats.
 * Required: employeeId, date
 * Optional: times, locations, flags
 */
@Data
public class AttendanceImport {
    private String employeeId;          // required
    private String date;                // required (flexible formats)
    private String clockInTime;         // optional (HH:mm, HH:mm:ss, h:mm a, etc.)
    private String clockOutTime;        // optional
    private String clockInLocation;     // optional
    private String clockOutLocation;    // optional
    private String clockInWorkingFrom;  // optional (Office / Remote)
    private String clockOutWorkingFrom; // optional
    private String late;                // optional boolean ("true"/"false" / "yes"/"no" / 1/0)
    private String halfDay;             // optional boolean
}
