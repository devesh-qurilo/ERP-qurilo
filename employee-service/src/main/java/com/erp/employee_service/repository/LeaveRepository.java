package com.erp.employee_service.repository;

import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.leave.Leave;
import com.erp.employee_service.entity.leave.LeaveStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    List<Leave> findByEmployeeEmployeeIdOrderByCreatedAtDesc(String employeeId);

    List<Leave> findByStatusOrderByCreatedAtDesc(LeaveStatus status);

    List<Leave> findByEmployeeDepartmentIdOrderByCreatedAtDesc(Long departmentId);

    @Query("SELECT l FROM Leave l WHERE l.status = 'APPROVED' AND " +
            "(:date BETWEEN l.startDate AND l.endDate OR l.singleDate = :date)")
    List<Leave> findApprovedLeavesByDate(@Param("date") LocalDate date);

    @Query("SELECT l FROM Leave l WHERE l.employee.employeeId = :employeeId AND " +
            "l.status = 'APPROVED' AND " +
            "(:date BETWEEN l.startDate AND l.endDate OR l.singleDate = :date)")
    Optional<Leave> findApprovedLeaveByEmployeeAndDate(@Param("employeeId") String employeeId,
                                                       @Param("date") LocalDate date);

    @Query("SELECT l FROM Leave l WHERE l.status = 'PENDING' ORDER BY l.createdAt DESC")
    List<Leave> findAllPendingLeaves();

    long countByStatus(LeaveStatus status);

    Optional<Leave> findFirstByEmployeeAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Employee emp, String approved, LocalDate date, LocalDate date1);

    //Attendance Management
    List<Leave> findByEmployeeAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Employee employee, LeaveStatus status, LocalDate date1, LocalDate date2);

    // helper for single day leaves:
    List<Leave> findByEmployeeAndStatusAndSingleDate(Employee employee, LeaveStatus status, LocalDate singleDate);

    boolean existsByEmployeeAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Employee emp, LeaveStatus leaveStatus, LocalDate date, LocalDate date1);

    boolean existsByEmployeeAndStatusAndSingleDate(Employee emp, LeaveStatus leaveStatus, LocalDate date);

    void deleteByEmployeeEmployeeId(String employeeId);
    @Modifying
    @Transactional
    @Query("DELETE FROM Leave l WHERE l.employee.employeeId = :empId")
    void deleteByEmployeeId(@Param("empId") String empId);

    @Modifying
    @Transactional
    @Query("""
DELETE FROM Leave l
WHERE l.employee.employeeId = :empId
   OR l.approvedBy.employeeId = :empId
""")
    void deleteAllForEmployee(@Param("empId") String empId);

}