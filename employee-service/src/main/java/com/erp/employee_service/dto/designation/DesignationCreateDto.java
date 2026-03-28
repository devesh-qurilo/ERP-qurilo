package com.erp.employee_service.dto.designation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DesignationCreateDto {
    @NotBlank
    private String designationName;
    private Long parentDesignationId;
}
