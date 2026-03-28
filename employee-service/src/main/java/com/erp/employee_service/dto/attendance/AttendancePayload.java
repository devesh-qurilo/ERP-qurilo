package com.erp.employee_service.dto.attendance;

import lombok.Data;

import java.time.LocalTime;

@Data
public class AttendancePayload {
    private LocalTime clockInTime;
    private String clockInLocation;
    private String clockInWorkingFrom;

    private LocalTime clockOutTime;
    private String clockOutLocation;
    private String clockOutWorkingFrom;

    private Boolean late = false;
    private Boolean halfDay = false;
}