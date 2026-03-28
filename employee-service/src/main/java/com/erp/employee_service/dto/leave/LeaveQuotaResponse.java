package com.erp.employee_service.dto.leave;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveQuotaResponse {
    private Long id;
    private String leaveType;
    private Integer totalLeaves;
    private Integer monthlyLimit;
    private Integer totalTaken;
    private Integer overUtilized;
    private Integer remainingLeaves;
}
