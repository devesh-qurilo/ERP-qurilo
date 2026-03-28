package com.erp.employee_service.service.appreciation;

import com.erp.employee_service.dto.appreciation.AppreciationRequestDto;
import com.erp.employee_service.dto.appreciation.AppreciationResponseDto;

import java.util.List;

public interface AppreciationService {
    AppreciationResponseDto create(String adminEmployeeId, AppreciationRequestDto dto);
    List<AppreciationResponseDto> getAll();
    List<AppreciationResponseDto> getForEmployee(String employeeId);
    AppreciationResponseDto getById(Long id);
    AppreciationResponseDto update(String adminEmployeeId, Long id, AppreciationRequestDto dto);
    void delete(String adminEmployeeId, Long id);
}
