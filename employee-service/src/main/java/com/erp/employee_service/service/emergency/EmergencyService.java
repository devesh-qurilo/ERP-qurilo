package com.erp.employee_service.service.emergency;

import com.erp.employee_service.dto.emergency.EmergencyContactRequestDto;
import com.erp.employee_service.dto.emergency.EmergencyContactResponseDto;
import com.erp.employee_service.dto.emergency.UpdateEmergencyContactDto;

import java.util.List;

public interface EmergencyService {
    EmergencyContactResponseDto createEmergencyContact(String employeeId, EmergencyContactRequestDto dto);
    List<EmergencyContactResponseDto> getAllByEmployeeId(String employeeId);
    EmergencyContactResponseDto getByEmployeeIdAndContactId(String employeeId, Long id);
    EmergencyContactResponseDto updateEmergencyContact(String employeeId, Long id, UpdateEmergencyContactDto dto);
    void deleteByEmployeeIdAndContactId(String employeeId, Long id);
}
