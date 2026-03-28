package com.erp.employee_service.repository;

import com.erp.employee_service.entity.emergency.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyRepository extends JpaRepository<EmergencyContact, Long> {
    List<EmergencyContact> findByEmployee_EmployeeId(String employeeId);
    Optional<EmergencyContact> findByIdAndEmployee_EmployeeId(Long id, String employeeId);

    List<EmergencyContact> findByEmployeeEmployeeId(String employeeId);
    void deleteByEmployeeEmployeeId(String employeeId);
}
