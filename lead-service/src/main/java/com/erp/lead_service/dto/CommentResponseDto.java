package com.erp.lead_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponseDto {
    private Long id;
    private String employeeId;
    private String commentText;
    private LocalDateTime createdAt;
}
