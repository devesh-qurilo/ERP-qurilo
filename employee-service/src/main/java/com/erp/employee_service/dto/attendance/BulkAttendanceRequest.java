package com.erp.employee_service.dto.attendance;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BulkAttendanceRequest {
    private List<String> employeeIds;
    private List<LocalDate> dates; // exact dates to mark
    private AttendancePayload payload;
    private boolean overwrite = false;
    private String markedBy; // admin employeeId who marks
}