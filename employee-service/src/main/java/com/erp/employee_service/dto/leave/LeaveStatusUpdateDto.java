package com.erp.employee_service.dto.leave;

import com.erp.employee_service.entity.leave.LeaveStatus;
import lombok.Data;

@Data
public class LeaveStatusUpdateDto {
    private LeaveStatus status;
    private String rejectionReason; // Required if status is REJECTED
}