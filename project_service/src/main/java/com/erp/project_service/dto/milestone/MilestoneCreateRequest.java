package com.erp.project_service.dto.milestone;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MilestoneCreateRequest {
    @NotBlank
    private String title;
    private BigDecimal milestoneCost;
    private String status; // PLANNED / IN_PROGRESS / COMPLETED / CANCELLED
    private String summary;
    private LocalDate startDate;
    private LocalDate endDate;
}
