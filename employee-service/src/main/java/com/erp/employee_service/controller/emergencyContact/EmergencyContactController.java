package com.erp.employee_service.controller.emergencyContact;

import com.erp.employee_service.dto.emergency.EmergencyContactRequestDto;
import com.erp.employee_service.dto.emergency.EmergencyContactResponseDto;
import com.erp.employee_service.dto.emergency.UpdateEmergencyContactDto;
import com.erp.employee_service.service.emergency.EmergencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee/{employeeId}/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final EmergencyService emergencyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or #employeeId == authentication.name")
    public ResponseEntity<EmergencyContactResponseDto> create(
            @PathVariable String employeeId,
            @RequestBody EmergencyContactRequestDto requestDto) {
        return new ResponseEntity<>(emergencyService.createEmergencyContact(employeeId, requestDto), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or #employeeId == authentication.name")
    public ResponseEntity<List<EmergencyContactResponseDto>> getAll(@PathVariable String employeeId) {
        return ResponseEntity.ok(emergencyService.getAllByEmployeeId(employeeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #employeeId == authentication.name")
    public ResponseEntity<EmergencyContactResponseDto> getById(
            @PathVariable String employeeId,
            @PathVariable Long id) {
        return ResponseEntity.ok(emergencyService.getByEmployeeIdAndContactId(employeeId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #employeeId == authentication.name")
    public ResponseEntity<EmergencyContactResponseDto> update(
            @PathVariable String employeeId,
            @PathVariable Long id,
            @RequestBody UpdateEmergencyContactDto updateDto) {
        return ResponseEntity.ok(emergencyService.updateEmergencyContact(employeeId, id, updateDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #employeeId == authentication.name")
    public ResponseEntity<Void> delete(
            @PathVariable String employeeId,
            @PathVariable Long id) {
        emergencyService.deleteByEmployeeIdAndContactId(employeeId, id);
        return ResponseEntity.ok().build();
    }
}
