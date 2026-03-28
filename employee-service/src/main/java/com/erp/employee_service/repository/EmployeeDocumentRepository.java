package com.erp.employee_service.repository;

import com.erp.employee_service.entity.EmployeeDocument;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {
    List<EmployeeDocument> findByEmployeeId(String employeeId);
    @Modifying
    @Transactional
    @Query("DELETE FROM EmployeeDocument d WHERE d.employeeId = :empId")
    void deleteByEmployeeId(@Param("empId") String empId);

}
