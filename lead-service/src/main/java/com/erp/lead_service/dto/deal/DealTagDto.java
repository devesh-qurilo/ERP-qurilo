package com.erp.lead_service.dto.deal;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DealTagDto {
    private Long id;
    private String tagName;
    private LocalDateTime createdAt;
}