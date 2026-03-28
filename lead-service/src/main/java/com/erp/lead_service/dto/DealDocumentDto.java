package com.erp.lead_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DealDocumentDto {
    private Long id;
    private String filename;
    private String url;
    private LocalDateTime uploadedAt;
}
