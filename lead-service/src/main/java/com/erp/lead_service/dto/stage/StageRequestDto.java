package com.erp.lead_service.dto.stage;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StageRequestDto {
    @NotBlank
    private String name;
}
