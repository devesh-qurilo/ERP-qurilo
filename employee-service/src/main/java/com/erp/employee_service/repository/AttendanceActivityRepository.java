package com.erp.employee_service.repository;

import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.attendance.Attendance;
import com.erp.employee_service.entity.attendance.AttendanceActivity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceActivityRepository extends JpaRepository<AttendanceActivity, Long> {
    List<AttendanceActivity> findByEmployeeAndDateOrderByCreatedAtAsc(Employee employee, LocalDate date);
    List<AttendanceActivity> findByEmployeeAndDateBetweenOrderByDateAscCreatedAtAsc(Employee employee, LocalDate from, LocalDate to);

    List<AttendanceActivity> findByAttendance(Attendance attendance);

    @Modifying
    @Transactional
    void deleteByAttendance(Attendance attendance);
    @Modifying
    @Transactional
    @Query("DELETE FROM AttendanceActivity a WHERE a.employee.employeeId = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") String employeeId);
}
