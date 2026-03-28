package com.erp.employee_service.dto.department;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentCreateDto {
    @NotBlank
    private String departmentName;

    // optional parent department id
    private Long parentDepartmentId;
}
