package com.erp.employee_service.service.company;

import com.erp.employee_service.dto.company.CompanyRequestDto;
import com.erp.employee_service.dto.company.CompanyResponseDto;

public interface CompanyService {
    CompanyResponseDto createCompany(String adminEmployeeId, CompanyRequestDto dto);
    CompanyResponseDto getCompany();
    CompanyResponseDto updateCompany(String adminEmployeeId, CompanyRequestDto dto);
    void deleteCompany(String adminEmployeeId);
}