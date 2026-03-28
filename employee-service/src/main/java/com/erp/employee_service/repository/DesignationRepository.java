package com.erp.employee_service.repository;

import com.erp.employee_service.entity.designation.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {
    Optional<Designation> findByDesignationNameIgnoreCase(String name);
}
