package com.erp.employee_service.repository;

import com.erp.employee_service.entity.notification.Notification;
import com.erp.employee_service.entity.Employee;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverOrderByCreatedAtDesc(Employee receiver);

    void deleteBySenderEmployeeIdOrReceiverEmployeeId(String employeeId, String employeeId1);
    @Modifying
    @Transactional
    @Query("""
DELETE FROM Notification n
WHERE n.sender.employeeId = :empId
   OR n.receiver.employeeId = :empId
""")
    void deleteAllForEmployee(@Param("empId") String empId);


}
