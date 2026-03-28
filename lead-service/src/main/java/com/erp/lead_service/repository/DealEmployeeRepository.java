package com.erp.lead_service.repository;

import com.erp.lead_service.entity.DealEmployee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DealEmployeeRepository extends JpaRepository<DealEmployee, Long> {
    List<DealEmployee> findByDealId(Long dealId);
    Optional<DealEmployee> findByDealIdAndEmployeeId(Long dealId, String employeeId);
    void deleteByDealIdAndEmployeeId(Long dealId, String employeeId);
    boolean existsByDealIdAndEmployeeId(Long dealId, String employeeId);

    void deleteByDealId(Long id);
}
