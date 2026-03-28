package com.erp.lead_service.service;

import com.erp.lead_service.client.EmployeeServiceClient;
import com.erp.lead_service.dto.EmployeeMetaDto;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.DealEmployee;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.repository.DealEmployeeRepository;
import com.erp.lead_service.repository.DealRepository;
import com.erp.lead_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DealEmployeeServiceImpl implements DealEmployeeService {

    private final DealEmployeeRepository repo;
    private final DealRepository dealRepository;
    private final JwtUtil jwtUtil;

    // NEW: to fetch employee details
    private final EmployeeServiceClient employeeClient;

    @Override
    @Transactional
    public void assignEmployees(Long dealId, List<String> employeeIds, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can access deal employees");
        }
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found"));
        for (String eid : employeeIds) {
            if (!repo.existsByDealIdAndEmployeeId(dealId, eid)) {
                DealEmployee de = new DealEmployee();
                de.setDeal(deal);
                de.setEmployeeId(eid);
                repo.save(de);
            }
        }
    }

    @Override
    @Transactional
    public void removeEmployee(Long dealId, String employeeId, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can access deal employees");
        }
        repo.deleteByDealIdAndEmployeeId(dealId, employeeId);
    }

    @Override
    public List<EmployeeMetaDto> listEmployees(Long dealId, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can access deal employees");
        }

        var rows = repo.findByDealId(dealId);
        List<EmployeeMetaDto> out = new ArrayList<>();

        for (DealEmployee de : rows) {
            String eid = de.getEmployeeId();
            try {
                // assuming employeeClient.getEmployeeById returns fields used below
                var emp = employeeClient.getEmployeeById(eid, authHeader);

                EmployeeMetaDto meta = new EmployeeMetaDto();
                meta.setEmployeeId(emp.getEmployeeId());
                meta.setName(emp.getName());
                meta.setDesignation(emp.getDesignationName());
                meta.setDepartment(emp.getDepartmentName());
                meta.setProfileUrl(emp.getProfilePictureUrl());
                out.add(meta);
            } catch (Exception ex) {
                log.warn("Failed to fetch metadata for employee {}: {}", eid, ex.getMessage());
                // fallback with id only so the list is still complete
                EmployeeMetaDto meta = new EmployeeMetaDto();
                meta.setEmployeeId(eid);
                out.add(meta);
            }
        }
        return out;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null) return null;
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }
}
