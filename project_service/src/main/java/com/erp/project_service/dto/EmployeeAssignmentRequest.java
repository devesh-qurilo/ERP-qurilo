package com.erp.project_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class EmployeeAssignmentRequest {
    private List<String> employeeIds;
}
