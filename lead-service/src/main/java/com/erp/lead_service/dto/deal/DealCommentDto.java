package com.erp.lead_service.dto.deal;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DealCommentDto {
    private Long id;
    private String employeeId;
    private String commentText;
    private LocalDateTime createdAt;
}
