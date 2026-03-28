package com.erp.employee_service.dto.department;

import lombok.Data;

@Data
public class DepartmentUpdateDto {
    private String departmentName;
    private Long parentDepartmentId; // nullable to change/remove parent
}
