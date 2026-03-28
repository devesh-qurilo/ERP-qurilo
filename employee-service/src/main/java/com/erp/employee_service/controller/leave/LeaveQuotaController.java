package com.erp.employee_service.controller.leave;

import com.erp.employee_service.dto.leave.LeaveQuotaResponse;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.leave.LeaveQuota;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.service.leave.LeaveQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employee/leave-quota")
@RequiredArgsConstructor
public class LeaveQuotaController {

    private final EmployeeRepository employeeRepository;
    private final LeaveQuotaService leaveQuotaService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<?> getMyQuotas(Authentication authentication) {
        String employeeId = authentication.getName(); // ensure this is employeeId in your JWT principal
        return employeeRepository.findById(employeeId)
                .map(emp -> {
                    List<LeaveQuota> quotas = leaveQuotaService.getQuotasForEmployee(emp);
                    List<LeaveQuotaResponse> resp = quotas.stream().map(this::toDto).collect(Collectors.toList());
                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> ResponseEntity.status(404).build());
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<LeaveQuotaResponse>> getLeaveQuotasByEmployeeId(@PathVariable String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElse(null);

        List<LeaveQuotaResponse> quotas = leaveQuotaService.getQuotasByEmployeeId(employeeId);
        return ResponseEntity.ok(quotas);
    }

    private LeaveQuotaResponse toDto(LeaveQuota q) {
        return LeaveQuotaResponse.builder()
                .id(q.getId())
                .leaveType(q.getLeaveType())
                .totalLeaves(q.getTotalLeaves())
                .monthlyLimit(q.getMonthlyLimit())
                .totalTaken(q.getTotalTaken())
                .overUtilized(q.getOverUtilized())
                .remainingLeaves(q.getRemainingLeaves())
                .build();
    }
}
