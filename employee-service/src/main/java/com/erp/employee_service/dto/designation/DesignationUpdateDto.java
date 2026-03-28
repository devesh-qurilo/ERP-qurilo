package com.erp.employee_service.dto.designation;

import lombok.Data;

@Data
public class DesignationUpdateDto {
    private String designationName;
    private Long parentDesignationId;
}
