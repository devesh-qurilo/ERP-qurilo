package com.erp.lead_service.service;

import com.erp.lead_service.dto.EmployeeMetaDto;
import com.erp.lead_service.dto.employee.EmployeeDto;

import java.util.List;

public interface DealEmployeeService {
    void assignEmployees(Long dealId, java.util.List<String> employeeIds, String authHeader);
    void removeEmployee(Long dealId, String employeeId, String authHeader);
    List<EmployeeMetaDto> listEmployees(Long dealId, String authHeader);
}
