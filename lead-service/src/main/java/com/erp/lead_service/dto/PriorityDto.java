package com.erp.lead_service.dto;

import lombok.Data;

@Data
public class PriorityDto {
    private Long id;
    private String status;
    private String color;
    private Long dealId;
    private Boolean isGlobal = true;
}
