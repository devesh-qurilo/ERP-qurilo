package com.erp.auth_service.repository;

import com.erp.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmployeeId(String employeeId);
}
