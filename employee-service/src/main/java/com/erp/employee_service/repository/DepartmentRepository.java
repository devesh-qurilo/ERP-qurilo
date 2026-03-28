package com.erp.employee_service.repository;

import com.erp.employee_service.entity.department.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByDepartmentNameIgnoreCase(String name);
    List<Department> findByParentDepartmentId(Long parentId);
    List<Department> findByParentDepartment(Department parent);
}
