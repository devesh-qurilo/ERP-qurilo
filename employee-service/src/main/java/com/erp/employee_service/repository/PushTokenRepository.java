package com.erp.employee_service.repository;
import com.erp.employee_service.entity.pushNotification.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
    List<PushToken> findByEmployeeId(String employeeId);
    Optional<PushToken> findByToken(String token);
    void deleteByToken(String token);
    void deleteByEmployeeId(String employeeId);

}
