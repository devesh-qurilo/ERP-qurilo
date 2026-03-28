package com.erp.employee_service.repository;

import com.erp.employee_service.entity.promotion.Promotion;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByEmployee_EmployeeId(String employeeId);
    List<Promotion> findAllByOrderByCreatedAtDesc();
    @Modifying
    @Transactional
    @Query("DELETE FROM Promotion p WHERE p.employee.employeeId = :empId")
    void deleteByEmployeeId(@Param("empId") String empId);

}