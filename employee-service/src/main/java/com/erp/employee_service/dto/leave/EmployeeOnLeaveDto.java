package com.erp.employee_service.dto.leave;

import com.erp.employee_service.entity.leave.LeaveType;
import lombok.Data;

@Data
public class EmployeeOnLeaveDto {
    private String employeeId;
    private String employeeName;
    private String department;
    private LeaveType leaveType;
}
