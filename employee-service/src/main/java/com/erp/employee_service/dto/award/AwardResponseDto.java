package com.erp.employee_service.dto.award;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AwardResponseDto {

    private Long id;
    private String title;
    private String summary;
    private String iconUrl;
    private Long iconFileId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}