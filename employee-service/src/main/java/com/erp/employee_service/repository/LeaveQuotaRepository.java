package com.erp.employee_service.repository;

import com.erp.employee_service.entity.leave.LeaveQuota;
import com.erp.employee_service.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaveQuotaRepository extends JpaRepository<LeaveQuota, Long> {
    List<LeaveQuota> findByEmployee(Employee employee);
    Optional<LeaveQuota> findByEmployeeAndLeaveType(Employee employee, String leaveType);
    boolean existsByEmployeeAndLeaveType(Employee employee, String leaveType);

    List<LeaveQuota> findByEmployeeEmployeeId(String employeeId);

    Optional<LeaveQuota> findByEmployeeEmployeeIdAndLeaveTypeAndYear(
            String employeeId, String leaveType, Integer year);

   // Leave
    @Query("SELECT lq FROM LeaveQuota lq WHERE lq.employee.employeeId = :employeeId AND lq.year = :year")
    List<LeaveQuota> findByEmployeeAndYear(@Param("employeeId") String employeeId, @Param("year") Integer year);

    boolean existsByEmployeeEmployeeIdAndLeaveTypeAndYear(String employeeId, String leaveType, Integer year);

    void deleteByEmployeeEmployeeId(String employeeId);
}
