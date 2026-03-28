package com.erp.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private String employeeId;
    private String name;
    private String profileUrl;
    private String designation;
    private String department;
}