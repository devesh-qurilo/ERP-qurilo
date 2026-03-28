package com.erp.employee_service.service.promotion;

import com.erp.employee_service.dto.notification.SendNotificationDto;
import com.erp.employee_service.dto.promotion.PromotionRequestDto;
import com.erp.employee_service.dto.promotion.PromotionResponseDto;
import com.erp.employee_service.dto.promotion.PromotionUpdateDto;
import com.erp.employee_service.entity.department.Department;
import com.erp.employee_service.entity.designation.Designation;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.promotion.Promotion;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.DepartmentRepository;
import com.erp.employee_service.repository.DesignationRepository;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.repository.PromotionRepository;
import com.erp.employee_service.service.notification.NotificationService;
import jakarta.validation.Valid;
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
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final NotificationService notificationService;

    public PromotionResponseDto createPromotion(String employeeId, PromotionRequestDto requestDto) {
        // Validate employee
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        // Validate new department
        Department newDepartment = departmentRepository.findById(requestDto.getNewDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + requestDto.getNewDepartmentId()));

        // Validate new designation
        Designation newDesignation = designationRepository.findById(requestDto.getNewDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with ID: " + requestDto.getNewDesignationId()));

        // Get current department and designation from employee
        Department oldDepartment = employee.getDepartment();
        Designation oldDesignation = employee.getDesignation();

        if (oldDepartment == null || oldDesignation == null) {
            throw new IllegalStateException("Employee does not have current department or designation assigned");
        }

        // Create promotion record
        Promotion promotion = Promotion.builder()
                .employee(employee)
                .oldDepartment(oldDepartment)
                .oldDesignation(oldDesignation)
                .newDepartment(newDepartment)
                .newDesignation(newDesignation)
                .isPromotion(requestDto.getIsPromotion())
                .sendNotification(requestDto.getSendNotification())
                .remarks(requestDto.getRemarks())
                .build();

        Promotion savedPromotion = promotionRepository.save(promotion);

        // Update employee with new department and designation
        employee.setDepartment(newDepartment);
        employee.setDesignation(newDesignation);
        employeeRepository.save(employee);

        // Send notification if requested
        if (requestDto.getSendNotification()) {
            sendPromotionNotification(employee, savedPromotion);
        }

        return mapToResponseDto(savedPromotion);
    }

    public PromotionResponseDto updatePromotion(Long id, PromotionUpdateDto updateDto) {
        // Find existing promotion
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion record not found with ID: " + id));

        // Validate new department
        Department newDepartment = departmentRepository.findById(updateDto.getNewDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + updateDto.getNewDepartmentId()));

        // Validate new designation
        Designation newDesignation = designationRepository.findById(updateDto.getNewDesignationId())
                .orElseThrow(() -> new ResourceNotFoundException("Designation not found with ID: " + updateDto.getNewDesignationId()));

        Employee employee = promotion.getEmployee();

        // Track old values for notification comparison
        Department oldDepartmentBeforeUpdate = promotion.getNewDepartment();
        Designation oldDesignationBeforeUpdate = promotion.getNewDesignation();
        Boolean oldIsPromotion = promotion.getIsPromotion();

        // Update promotion record
        promotion.setNewDepartment(newDepartment);
        promotion.setNewDesignation(newDesignation);
        promotion.setIsPromotion(updateDto.getIsPromotion());
        promotion.setSendNotification(updateDto.getSendNotification());
        promotion.setRemarks(updateDto.getRemarks());

        Promotion updatedPromotion = promotionRepository.save(promotion);

        // Update employee with new department and designation
        employee.setDepartment(newDepartment);
        employee.setDesignation(newDesignation);
        employeeRepository.save(employee);


        return mapToResponseDto(updatedPromotion);
    }


    @Async
    public CompletableFuture<Void> sendPromotionNotification(Employee employee, Promotion promotion) {
        try {
            SendNotificationDto dto = new SendNotificationDto();
            dto.setReceiverEmployeeId(employee.getEmployeeId());

            if (promotion.getIsPromotion()) {
                dto.setTitle("Promotion Notification");
                dto.setMessage("Congratulations! You have been promoted from " +
                        promotion.getOldDesignation().getDesignationName() + " (" + promotion.getOldDepartment().getDepartmentName() +
                        ") to " + promotion.getNewDesignation().getDesignationName() + " (" +
                        promotion.getNewDepartment().getDepartmentName() + ").");
            } else {
                dto.setTitle("Demotion Notification");
                dto.setMessage("Your designation has been changed from " +
                        promotion.getOldDesignation().getDesignationName() + " (" + promotion.getOldDepartment().getDepartmentName() +
                        ") to " + promotion.getNewDesignation().getDesignationName() + " (" +
                        promotion.getNewDepartment().getDepartmentName() + ").");
            }

            dto.setType("PROMOTION");
            notificationService.sendNotification(null, dto);
        } catch (Exception e) {
            System.err.println("Failed to send promotion notification: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    public List<PromotionResponseDto> getAllPromotions() {
        return promotionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<PromotionResponseDto> getPromotionsByEmployee(String employeeId) {
        return promotionRepository.findByEmployee_EmployeeId(employeeId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public PromotionResponseDto getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion record not found with ID: " + id));
        return mapToResponseDto(promotion);
    }

    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion record not found with ID: " + id));
        promotionRepository.delete(promotion);
    }

    private PromotionResponseDto mapToResponseDto(Promotion promotion) {
        PromotionResponseDto dto = new PromotionResponseDto();
        dto.setId(promotion.getId());
        dto.setEmployeeId(promotion.getEmployee().getEmployeeId());
        dto.setEmployeeName(promotion.getEmployee().getName());
        dto.setOldDepartmentId(promotion.getOldDepartment().getId());
        dto.setOldDepartmentName(promotion.getOldDepartment().getDepartmentName());
        dto.setOldDesignationId(promotion.getOldDesignation().getId());
        dto.setOldDesignationName(promotion.getOldDesignation().getDesignationName());
        dto.setNewDepartmentId(promotion.getNewDepartment().getId());
        dto.setNewDepartmentName(promotion.getNewDepartment().getDepartmentName());
        dto.setNewDesignationId(promotion.getNewDesignation().getId());
        dto.setNewDesignationName(promotion.getNewDesignation().getDesignationName());
        dto.setIsPromotion(promotion.getIsPromotion());
        dto.setSendNotification(promotion.getSendNotification());
        dto.setCreatedAt(promotion.getCreatedAt());
        dto.setRemarks(promotion.getRemarks());
        return dto;
    }

}