package com.erp.employee_service.dto.leave;

import com.erp.employee_service.entity.leave.DurationType;
import com.erp.employee_service.entity.leave.LeaveStatus;
import com.erp.employee_service.entity.leave.LeaveType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LeaveResponseDto {
    private Long id;
    private String employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private DurationType durationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate singleDate;
    private String reason;
    private LeaveStatus status;
    private String rejectionReason;
    private String approvedByName;
    private Boolean isPaid;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private List<String> documentUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
