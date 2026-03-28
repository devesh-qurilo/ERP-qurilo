package com.erp.employee_service.dto.promotion;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PromotionResponseDto {

    private Long id;
    private String employeeId;
    private String employeeName;
    private Long oldDepartmentId;
    private String oldDepartmentName;
    private Long oldDesignationId;
    private String oldDesignationName;
    private Long newDepartmentId;
    private String newDepartmentName;
    private Long newDesignationId;
    private String newDesignationName;
    private Boolean isPromotion;
    private Boolean sendNotification;
    private LocalDateTime createdAt;
    private String remarks;
}