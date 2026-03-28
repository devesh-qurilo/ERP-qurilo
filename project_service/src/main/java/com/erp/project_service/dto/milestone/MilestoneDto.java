package com.erp.project_service.dto.milestone;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
public class MilestoneDto {
    private Long id;
    private Long projectId;
    private String title;
    private BigDecimal milestoneCost;
    private String status;
    private String summary;
    private LocalDate startDate;
    private LocalDate endDate;
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;
}
