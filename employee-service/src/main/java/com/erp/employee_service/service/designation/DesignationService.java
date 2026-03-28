package com.erp.employee_service.service.designation;

import com.erp.employee_service.dto.designation.DesignationCreateDto;
import com.erp.employee_service.dto.designation.DesignationResponseDto;
import com.erp.employee_service.dto.designation.DesignationUpdateDto;

import java.util.List;

public interface DesignationService {
    DesignationResponseDto create(DesignationCreateDto dto);
    DesignationResponseDto update(Long id, DesignationUpdateDto dto);
    DesignationResponseDto getById(Long id);
    List<DesignationResponseDto> getAll();
    void delete(Long id);
}
