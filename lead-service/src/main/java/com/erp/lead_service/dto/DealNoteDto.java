package com.erp.lead_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DealNoteDto {
    private Long id;
    private String noteTitle;
    private String noteType;
    private String noteDetails;
    private String createdBy;
    private LocalDateTime createdAt;
}
