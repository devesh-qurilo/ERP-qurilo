package com.erp.lead_service.dto.deal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DealCommentRequestDto {
    @NotBlank
    private String commentText;
}

