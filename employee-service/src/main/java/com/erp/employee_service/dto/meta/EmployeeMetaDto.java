package com.erp.employee_service.dto.meta;

import lombok.Data;

@Data
public class EmployeeMetaDto {
    private String employeeId;
    private String name;
    private String designation;  // designationName से map होगा
    private String department;   // departmentName से map होगा
    private String profileUrl;   // profilePictureUrl से map होगा
}
