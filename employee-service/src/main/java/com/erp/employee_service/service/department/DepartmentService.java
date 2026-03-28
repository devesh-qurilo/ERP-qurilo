package com.erp.employee_service.service.department;

import com.erp.employee_service.dto.department.DepartmentCreateDto;
import com.erp.employee_service.dto.department.DepartmentResponseDto;
import com.erp.employee_service.dto.department.DepartmentUpdateDto;

import java.util.List;

public interface DepartmentService {
    DepartmentResponseDto create(DepartmentCreateDto dto);
    DepartmentResponseDto update(Long id, DepartmentUpdateDto dto);
    DepartmentResponseDto getById(Long id);
    List<DepartmentResponseDto> getAll();
    void delete(Long id);
}
