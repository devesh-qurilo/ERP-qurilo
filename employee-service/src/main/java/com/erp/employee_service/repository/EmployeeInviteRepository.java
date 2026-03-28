package com.erp.employee_service.repository;

import com.erp.employee_service.entity.EmployeeInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeInviteRepository
        extends JpaRepository<EmployeeInvite, Long> {

    Optional<EmployeeInvite> findByToken(String token);
}
