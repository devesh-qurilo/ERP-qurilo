package com.erp.employee_service.repository;

import com.erp.employee_service.entity.FileMeta;
import com.erp.employee_service.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetaRepository extends JpaRepository<FileMeta, Long> {
    List<FileMeta> findByEmployee(Employee employee);
    List<FileMeta> findByEmployeeAndEntityType(Employee employee, String entityType);
    Optional<FileMeta> findByIdAndEmployee(Long id, Employee employee);

    @Query("SELECT f FROM FileMeta f WHERE f.employee = :employee AND f.entityType = 'PROFILE'")
    List<FileMeta> findProfileFilesByEmployee(@Param("employee") Employee employee);

    void deleteByEmployeeEmployeeId(String employeeId);
    List<FileMeta> findByEmployeeEmployeeId(String employeeId);
    List<FileMeta> findByEmployeeAndEntityTypeStartingWith(Employee employee, String prefix);



    // keep both Optional and List variants if you use both styles

    List<FileMeta> findAllByEmployeeAndEntityType(Employee employee, String entityType);


}
