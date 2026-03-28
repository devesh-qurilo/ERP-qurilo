package com.erp.employee_service.dto.leave;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class LeaveCalendarDto {
    private LocalDate date;
    private List<EmployeeOnLeaveDto> employeesOnLeave;
}

