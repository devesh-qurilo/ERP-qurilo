package com.erp.employee_service.repository;

import com.erp.employee_service.entity.appreciation.Appreciation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppreciationRepository extends JpaRepository<Appreciation, Long> {
    List<Appreciation> findByGivenTo_EmployeeIdOrderByDateDesc(String employeeId);
    void deleteByPhoto_Id(Long photoId);

    void deleteByGivenTo_EmployeeId(String employeeId);

}
