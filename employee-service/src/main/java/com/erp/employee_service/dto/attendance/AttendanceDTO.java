package com.erp.employee_service.dto.attendance;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceDTO {
    private LocalDate date;
    private String employeeId;
    private String employeeName;
    private String status; // You'll need to compute this based on entity data

    // new fields you requested
    private String profilePictureUrl;
    private String departmentName;
    private String designationName;

    // attendance details
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

    // marked by info
    private String markedById;
    private String markedByName;
    // flags
    private Boolean holiday = Boolean.FALSE;
    private Boolean leave = Boolean.FALSE;
    private Boolean isPresent = Boolean.FALSE;

    // Constructor from Entity
    public AttendanceDTO(com.erp.employee_service.entity.attendance.Attendance attendance) {
        if (attendance == null) return;

        this.date = attendance.getDate();

        // Map employee data
        // Map employee data (null-safe)
        if (attendance.getEmployee() != null) {
            var emp = attendance.getEmployee();
            this.employeeId = attendance.getEmployee().getEmployeeId();
            // Use getName(), not toString()
            String name = attendance.getEmployee().getName();
            this.employeeName = name != null ? name : attendance.getEmployee().getEmployeeId();

            // profile picture
            try {
                String pic = emp.getProfilePictureUrl();
                this.profilePictureUrl = (pic != null && !pic.isBlank()) ? pic : null;
            } catch (Exception ignored) {
                this.profilePictureUrl = null;
            }

            // department name (null-safe)
            try {
                var dept = emp.getDepartment();
                if (dept != null) {
                    String dname = dept.getDepartmentName();
                    this.departmentName = (dname != null && !dname.isBlank()) ? dname : null;
                }
            } catch (Exception ignored) {
                this.departmentName = null;
            }

            // designation name (null-safe)
            try {
                var des = emp.getDesignation();
                if (des != null) {
                    String ds = des.getDesignationName();
                    this.designationName = (ds != null && !ds.isBlank()) ? ds : null;
                }
            } catch (Exception ignored) {
                this.designationName = null;
            }

        }

        // Map status - you'll need to compute this based on your business logic
        this.status = computeStatus(attendance);

        this.attendanceId = attendance.getId();
        this.overwritten = attendance.getOverwritten();
        this.late = attendance.getLate();
        this.halfDay = attendance.getHalfDay();
        this.clockInTime = attendance.getClockInTime();
        this.clockInLocation = attendance.getClockInLocation();
        this.clockInWorkingFrom = attendance.getClockInWorkingFrom();
        this.clockOutTime = attendance.getClockOutTime();
        this.clockOutLocation = attendance.getClockOutLocation();
        this.clockOutWorkingFrom = attendance.getClockOutWorkingFrom();

        // Map marked by info
        if (attendance.getMarkedBy() != null) {
            this.markedById = attendance.getMarkedBy().getEmployeeId();
            String mName = attendance.getMarkedBy().getName();
            this.markedByName = mName != null ? mName : attendance.getMarkedBy().getEmployeeId();
        }

        // You'll need to set holiday and leave from external service or calculation
        this.holiday = Boolean.FALSE; // Set based on your logic
        this.leave = Boolean.FALSE;   // Set based on your logic
        this.isPresent = attendance.getIsPresent();
    }

    // Default constructor
    public AttendanceDTO() {
    }

    // Helper method to compute status
    private String computeStatus(com.erp.employee_service.entity.attendance.Attendance attendance) {
        if (Boolean.TRUE.equals(attendance.getOverwritten())) {
            return "PRESENT";
        }
        if (Boolean.TRUE.equals(attendance.getIsPresent())) {
            return "PRESENT";
        }
        // Add your logic for LEAVE, HOLIDAY, ABSENT based on other fields
        return "PRESENT"; // Default, adjust as needed
    }
}