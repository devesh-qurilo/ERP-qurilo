package com.erp.employee_service.dto.imports;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EmployeeImportRequest {
    private String employeeId;
    private String name;
    private String email;
    private String gender;
    private LocalDate joiningDate;
    private String mobile;
}
