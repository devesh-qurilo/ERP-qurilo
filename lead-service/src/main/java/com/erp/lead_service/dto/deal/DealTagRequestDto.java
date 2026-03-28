package com.erp.lead_service.dto.deal;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealTagRequestDto {
    @NotBlank
    private String tagName;
}
