package com.erp.employee_service.repository;

import com.erp.employee_service.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Optional<Employee> findByEmployeeId(String employeeId);
    Optional<Employee> findByEmail(String email);
    Page<Employee> findAll(Pageable pageable);
    void deleteByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);

    //Attendance

    List<Employee> findByDepartmentId(Long departmentId);

    //Leave
    List<Employee> findByRole(String role);

    // Search employees by name or email
    List<Employee> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);

    @Query("SELECT e FROM Employee e WHERE e.birthday IS NOT NULL AND MONTH(e.birthday) = :month AND DAY(e.birthday) = :day")
    List<Employee> findByBirthdayMonthAndDay(int month, int day);
}
