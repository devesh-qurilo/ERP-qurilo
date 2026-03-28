package com.erp.employee_service.service;

import com.erp.employee_service.dto.CreateEmployeeRequest;
import com.erp.employee_service.dto.EmployeeResponse;
import com.erp.employee_service.dto.UpdateEmployeeRequest;
import com.erp.employee_service.dto.imports.EmployeeImportRequest;
import com.erp.employee_service.dto.settings.Profile;
import com.erp.employee_service.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeService {
    EmployeeResponse createEmployee(CreateEmployeeRequest request);
    Page<EmployeeResponse> getAll(Pageable pageable);
    EmployeeResponse getByEmployeeId(String employeeId);
    EmployeeResponse updateByEmployeeId(String employeeId, UpdateEmployeeRequest request);
    EmployeeResponse updateRoleByEmployeeId(String employeeId, String role);
    void deleteByEmployeeId(String employeeId);
    EmployeeResponse getByEmployeeIdOrThrow(String employeeId);
    EmployeeResponse createEmployeeWithProfile(CreateEmployeeRequest req, MultipartFile file);
    EmployeeResponse updateByEmployeeIdWithProfile(String employeeId, UpdateEmployeeRequest req, MultipartFile file);

    //Profile Settings
    EmployeeResponse updateProfileByEmployeeIdSettings(String employeeId, Profile req, MultipartFile file);

    //Search employee for chat
    List<EmployeeResponse> searchEmployees(String query);

    List<EmployeeResponse> getEmployeesWithBirthday(LocalDate date);

    Employee createEmployees(EmployeeImportRequest req);

    List<EmployeeResponse> getAllEmployee();

    EmployeeResponse getEmployeeById(String employeeId);
}
