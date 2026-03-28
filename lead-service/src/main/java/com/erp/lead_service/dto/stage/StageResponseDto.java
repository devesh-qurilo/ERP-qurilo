package com.erp.lead_service.dto.stage;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StageResponseDto {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
