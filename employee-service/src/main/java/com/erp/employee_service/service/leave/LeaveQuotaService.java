package com.erp.employee_service.service.leave;

import com.erp.employee_service.dto.leave.LeaveQuotaResponse;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.leave.LeaveQuota;
import com.erp.employee_service.repository.LeaveQuotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveQuotaService {

    private final LeaveQuotaRepository leaveQuotaRepository;

    private static final List<String> DEFAULT_TYPES = Arrays.asList("SICK", "CASUAL", "EARNED");
    private static final int DEFAULT_TOTAL = 5;
    private static final int DEFAULT_MONTHLY_LIMIT = 2;

    /**
     * Assign default quotas only if not already present for that employee.
     */
    @Transactional
    public void assignDefaultsIfMissing(Employee employee) {
        if (employee == null) return;
        for (String type : DEFAULT_TYPES) {
            boolean exists = leaveQuotaRepository.existsByEmployeeAndLeaveType(employee, type);
            if (!exists) {
                LeaveQuota q = LeaveQuota.builder()
                        .employee(employee)
                        .leaveType(type)
                        .totalLeaves(DEFAULT_TOTAL)
                        .monthlyLimit(DEFAULT_MONTHLY_LIMIT)
                        .totalTaken(0)
                        .overUtilized(0)
                        .remainingLeaves(DEFAULT_TOTAL)
                        .build();
                leaveQuotaRepository.save(q);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<LeaveQuota> getQuotasForEmployee(Employee employee) {
        return leaveQuotaRepository.findByEmployee(employee);
    }

    public List<LeaveQuotaResponse> getQuotasByEmployeeId(String employeeId) {
        return leaveQuotaRepository.findByEmployeeEmployeeId(employeeId).stream().map(this::toDto).toList();
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

    @Transactional
    public void refreshLeaveQuotaForNewYear() {

        List<LeaveQuota> quotas = leaveQuotaRepository.findAll();

        for (LeaveQuota q : quotas) {

            int carryForward = q.getRemainingLeaves() - q.getOverUtilized();

            if (carryForward < 0) {
                carryForward = 0;
            }

            int newTotal = DEFAULT_TOTAL + carryForward;

            q.setTotalLeaves(newTotal);
            q.setRemainingLeaves(newTotal);
            q.setTotalTaken(0);
            q.setOverUtilized(0);
        }

        leaveQuotaRepository.saveAll(quotas);
    }

}
