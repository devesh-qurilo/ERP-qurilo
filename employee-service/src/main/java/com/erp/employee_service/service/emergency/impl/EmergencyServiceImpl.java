package com.erp.employee_service.service.emergency.impl;

import com.erp.employee_service.dto.emergency.EmergencyContactRequestDto;
import com.erp.employee_service.dto.emergency.EmergencyContactResponseDto;
import com.erp.employee_service.dto.emergency.UpdateEmergencyContactDto;
import com.erp.employee_service.dto.notification.SendNotificationDto;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.emergency.EmergencyContact;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.repository.EmergencyRepository;
import com.erp.employee_service.service.emergency.EmergencyService;
import com.erp.employee_service.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmergencyServiceImpl implements EmergencyService {

    private final EmergencyRepository repository;
    private final EmployeeRepository employeeRepo;
    private final NotificationService notificationService;

    @Override
    public EmergencyContactResponseDto createEmergencyContact(String employeeId, EmergencyContactRequestDto dto) {
        Employee employee = employeeRepo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        EmergencyContact e = new EmergencyContact();
        e.setName(dto.getName());
        e.setEmail(dto.getEmail());
        e.setMobile(dto.getMobile());
        e.setAddress(dto.getAddress());
        e.setRelationship(dto.getRelationship());
        e.setEmployee(employee);

        EmergencyContactResponseDto response = mapToResponseDto(repository.save(e));

        // Send notification for emergency contact creation
        sendEmergencyContactNotification(employeeId, dto.getName(), "CREATED");

        return response;
    }

    @Override
    public List<EmergencyContactResponseDto> getAllByEmployeeId(String employeeId) {
        return repository.findByEmployee_EmployeeId(employeeId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public EmergencyContactResponseDto getByEmployeeIdAndContactId(String employeeId, Long id) {
        EmergencyContact contact = repository.findByIdAndEmployee_EmployeeId(id, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found with id: " + id));
        return mapToResponseDto(contact);
    }

    @Override
    public EmergencyContactResponseDto updateEmergencyContact(String employeeId, Long id, UpdateEmergencyContactDto dto) {
        EmergencyContact contact = repository.findByIdAndEmployee_EmployeeId(id, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found with id: " + id));

        String oldName = contact.getName();

        if (dto.getName() != null) contact.setName(dto.getName());
        if (dto.getEmail() != null) contact.setEmail(dto.getEmail());
        if (dto.getMobile() != null) contact.setMobile(dto.getMobile());
        if (dto.getAddress() != null) contact.setAddress(dto.getAddress());
        if (dto.getRelationship() != null) contact.setRelationship(dto.getRelationship());

        EmergencyContactResponseDto response = mapToResponseDto(repository.save(contact));

        // Send notification for emergency contact update
        sendEmergencyContactNotification(employeeId, contact.getName(), "UPDATED");

        return response;
    }

    @Override
    public void deleteByEmployeeIdAndContactId(String employeeId, Long id) {
        EmergencyContact contact = repository.findByIdAndEmployee_EmployeeId(id, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found with id: " + id));

        String contactName = contact.getName();
        repository.delete(contact);

        // Send notification for emergency contact deletion
        sendEmergencyContactNotification(employeeId, contactName, "DELETED");
    }

    @Async
    public CompletableFuture<Void> sendEmergencyContactNotification(String employeeId, String contactName, String action) {
        try {
            SendNotificationDto dto = new SendNotificationDto();
            dto.setReceiverEmployeeId(employeeId);

            switch (action) {
                case "CREATED":
                    dto.setTitle("Emergency Contact Added");
                    dto.setMessage("Emergency contact '" + contactName + "' has been added to your profile.");
                    break;
                case "UPDATED":
                    dto.setTitle("Emergency Contact Updated");
                    dto.setMessage("Emergency contact '" + contactName + "' has been updated.");
                    break;
                case "DELETED":
                    dto.setTitle("Emergency Contact Removed");
                    dto.setMessage("Emergency contact '" + contactName + "' has been removed from your profile.");
                    break;
                default:
                    dto.setTitle("Emergency Contact Modified");
                    dto.setMessage("Changes have been made to your emergency contact '" + contactName + "'.");
            }

            dto.setType("EMERGENCY_CONTACT");
            notificationService.sendNotification(null, dto);
        } catch (Exception e) {
            // Log error but don't throw exception to avoid affecting main operation
            System.err.println("Failed to send emergency contact notification: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    private EmergencyContactResponseDto mapToResponseDto(EmergencyContact e) {
        EmergencyContactResponseDto dto = new EmergencyContactResponseDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setEmail(e.getEmail());
        dto.setMobile(e.getMobile());
        dto.setAddress(e.getAddress());
        dto.setRelationship(e.getRelationship());
        dto.setEmployeeId(e.getEmployee().getEmployeeId());
        return dto;
    }
}