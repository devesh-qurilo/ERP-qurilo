package com.erp.employee_service.dto.promotion;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PromotionRequestDto {

    // Removed employeeId from here since it will come from path parameter

    @NotNull(message = "New department ID is required")
    private Long newDepartmentId;

    @NotNull(message = "New designation ID is required")
    private Long newDesignationId;

    @NotNull(message = "Send notification flag is required")
    private Boolean sendNotification;

    @NotNull(message = "Promotion flag is required")
    private Boolean isPromotion;

    private String remarks;
}