package com.erp.employee_service.repository;

import com.erp.employee_service.entity.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findFirstByIsActiveTrue();
    boolean existsByCompanyName(String companyName);
    boolean existsByEmail(String email);
}