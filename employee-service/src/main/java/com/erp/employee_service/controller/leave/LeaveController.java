package com.erp.employee_service.controller.leave;

import com.erp.employee_service.dto.leave.*;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.service.leave.LeaveService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final ObjectMapper objectMapper;
    private final EmployeeRepository employeeRepository;

@PostMapping(value = "/apply", consumes = "multipart/form-data")
public ResponseEntity<?> applyLeave(
        @AuthenticationPrincipal String employeeId,
        @RequestPart("leaveData") String leaveDataJson,
        @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {

    try {
        LeaveRequestDto requestDto = objectMapper.readValue(leaveDataJson, LeaveRequestDto.class);
        return ResponseEntity.ok(leaveService.applyLeave(employeeId, requestDto, documents));
    } catch (JsonProcessingException e) {
        return ResponseEntity.badRequest().body(
                Map.of("error", "Invalid JSON format", "details", e.getMessage())
        );
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to process leave application", "details", e.getMessage())
        );
    }
}

    @PostMapping(value = "/admin/apply", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaveResponseDto>> applyLeavesForEmployees(
            @AuthenticationPrincipal String adminId,
            @RequestPart("leaveData") String leaveDataJson,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {

        try {
            AdminLeaveRequestDto requestDto = objectMapper.readValue(leaveDataJson, AdminLeaveRequestDto.class);
            return ResponseEntity.ok(leaveService.applyLeavesForEmployees(requestDto, documents, adminId));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse leave data", e);
        }
    }

    @GetMapping("/my-leaves")
    public ResponseEntity<List<LeaveResponseDto>> getMyLeaves(
            @AuthenticationPrincipal String employeeId) {
        return ResponseEntity.ok(leaveService.getMyLeaves(employeeId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaveResponseDto>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaveResponseDto>> getPendingLeaves() {
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }

    @PatchMapping("/{leaveId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveResponseDto> updateLeaveStatus(
            @PathVariable Long leaveId,
            @AuthenticationPrincipal String adminId,
            @Valid @RequestBody LeaveStatusUpdateDto statusDto) {
        return ResponseEntity.ok(leaveService.updateLeaveStatus(leaveId, statusDto, adminId));
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<LeaveCalendarDto>> getLeaveCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(leaveService.getLeaveCalendar(date));
    }

    @DeleteMapping("/{leaveId}")
    public ResponseEntity<Void> deleteLeave(
            @PathVariable Long leaveId,
            @AuthenticationPrincipal String employeeId) {
        leaveService.deleteLeave(leaveId, employeeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{leaveId}")
    public ResponseEntity<LeaveResponseDto> getLeaveById(
            @PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveService.getLeaveById(leaveId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{leaveId}/documents/{documentId}")
    public ResponseEntity<Void> deleteLeaveDocument(
            @PathVariable Long leaveId,
            @PathVariable Long documentId) {
        leaveService.deleteLeaveDocument(leaveId, documentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<LeaveResponseDto>> getLeavesForEmployee(@PathVariable String employeeId) {
        // validate employee exists (better UX than returning empty)
        Employee emp = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        List<LeaveResponseDto> leaves = leaveService.getMyLeaves(employeeId);
        return ResponseEntity.ok(leaves);
    }

}