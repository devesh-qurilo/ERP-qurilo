package com.erp.employee_service.dto.leave;

import com.erp.employee_service.entity.leave.DurationType;
import com.erp.employee_service.entity.leave.LeaveStatus;
import com.erp.employee_service.entity.leave.LeaveType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Data
public class LeaveRequestDto {
    private LeaveType leaveType;
    private DurationType durationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate singleDate;
    private String reason;
    private List<MultipartFile> documents;// For file uploads
}