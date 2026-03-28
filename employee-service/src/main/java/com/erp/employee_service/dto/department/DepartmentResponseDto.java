package com.erp.employee_service.dto.department;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DepartmentResponseDto {
    private Long id;
    private String departmentName;
    private Long parentDepartmentId;
    private String parentDepartmentName;
    private LocalDate createAt;
}
