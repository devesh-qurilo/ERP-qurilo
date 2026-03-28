package com.erp.employee_service.repository;

import com.erp.employee_service.entity.attendance.Attendance;
import com.erp.employee_service.entity.Employee;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployeeAndDate(Employee employee, LocalDate date);
    boolean existsByEmployeeAndDate(Employee employee, LocalDate date);
    List<Attendance> findAllByEmployeeAndDateBetween(Employee employee, LocalDate from, LocalDate to);

    // used in service
    List<Attendance> findAllByEmployeeInAndDateBetween(List<Employee> employees, LocalDate from, LocalDate to);

    // used for employee /me endpoint to get all saved attendances for an employee
    List<Attendance> findAllByEmployee(Employee employee);

    // Find all attendances for multiple employees in date range
    @Query("SELECT a FROM Attendance a WHERE a.employee.employeeId IN :employeeIds AND a.date BETWEEN :startDate AND :endDate")
    List<Attendance> findByEmployeeIdsAndDateBetween(@Param("employeeIds") List<String> employeeIds,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    // Check if attendance exists for employee on specific date
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Attendance a WHERE a.employee.employeeId = :employeeId AND a.date = :date")
    boolean existsByEmployeeIdAndDate(@Param("employeeId") String employeeId, @Param("date") LocalDate date);

    // Delete attendance by employee and date
    void deleteByEmployeeAndDate(Employee employee, LocalDate date);

    // Find attendance by employee ID and date range
    @Query("SELECT a FROM Attendance a WHERE a.employee.employeeId = :employeeId AND a.date BETWEEN :startDate AND :endDate ORDER BY a.date")
    List<Attendance> findByEmployeeIdAndDateRange(@Param("employeeId") String employeeId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @EntityGraph(attributePaths = {"employee", "markedBy"})
    List<Attendance> findAll();

    // Or with a custom query
    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee LEFT JOIN FETCH a.markedBy")
    List<Attendance> findAllWithEmployees();


    @Query("select a from Attendance a where a.date = :date " +
            "and (lower(coalesce(a.clockInWorkingFrom,'')) like concat('%', lower(:keyword), '%') " +
            "or lower(coalesce(a.clockOutWorkingFrom,'')) like concat('%', lower(:keyword), '%'))")
    List<Attendance> findByDateAndWorkingFromLike(@Param("date") LocalDate date,
                                                  @Param("keyword") String keyword);

    /**
     * Same as above but for a date range [from..to]
     */
    @Query("select a from Attendance a where a.date >= :from and a.date <= :to " +
            "and (lower(coalesce(a.clockInWorkingFrom,'')) like concat('%', lower(:keyword), '%') " +
            "or lower(coalesce(a.clockOutWorkingFrom,'')) like concat('%', lower(:keyword), '%'))")
    List<Attendance> findByDateBetweenAndWorkingFromLike(@Param("from") LocalDate from,
                                                         @Param("to") LocalDate to,
                                                         @Param("keyword") String keyword);

    @Modifying
    @Transactional
    @Query("DELETE FROM Attendance a WHERE a.employee.employeeId = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") String employeeId);
}