package com.erp.chat_service.service;

import com.erp.chat_service.client.EmployeeClient;
import com.erp.chat_service.dto.EmployeeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmployeeService {

    @Autowired
    private EmployeeClient employeeClient;

    @Value("${employee.service.url:http://localhost:8083}")
    private String employeeServiceUrl;

    public EmployeeDTO getEmployeeMeta(String employeeId) {
        try {
            log.debug("Fetching employee meta for ID: {} from {}", employeeId, employeeServiceUrl);
            EmployeeDTO employee = employeeClient.getMeta(employeeId);

            if (employee == null) {
                log.warn("Employee service returned null for ID: {}", employeeId);
                return createMinimalEmployee(employeeId);
            }

            // Validate required fields
            if (employee.getName() == null) {
                employee.setName("Employee " + employeeId);
            }

            log.debug("Successfully fetched employee: {} - {}", employeeId, employee.getName());
            return employee;

        } catch (Exception e) {
            log.error("Error fetching employee meta for ID: {}. Error: {}", employeeId, e.getMessage());

            // Return minimal employee data - no mock names/designations
            return createMinimalEmployee(employeeId);
        }
    }

    private EmployeeDTO createMinimalEmployee(String employeeId) {
        EmployeeDTO minimalEmployee = new EmployeeDTO();
        minimalEmployee.setEmployeeId(employeeId);
        minimalEmployee.setName("Employee " + employeeId); // Only ID-based name
        minimalEmployee.setProfileUrl(null);
        minimalEmployee.setDesignation(null); // Keep null - don't mock
        minimalEmployee.setDepartment(null);  // Keep null - don't mock
        return minimalEmployee;
    }

    public boolean isEmployeeServiceAvailable() {
        try {
            EmployeeDTO employee = employeeClient.getMeta("EMP-TEST");
            return employee != null;
        } catch (Exception e) {
            log.warn("Employee service is not available: {}", e.getMessage());
            return false;
        }
    }
}