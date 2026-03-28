package com.erp.project_service.repository;

import com.erp.project_service.entity.Project;
import com.erp.project_service.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByShortCode(String shortCode);

    Page<Project> findByProjectStatus(ProjectStatus status, Pageable pageable);

    Page<Project> findByAssignedEmployeeIdsContains(String employeeId, Pageable pageable);
    boolean findByAssignedEmployeeIdsContains(String employeeIds);

    // ✅ FIX: Change return type to check if employee is assigned to project
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p WHERE p.id = :projectId AND :employeeId MEMBER OF p.assignedEmployeeIds")
    boolean isEmployeeAssignedToProject(@Param("projectId") Long projectId, @Param("employeeId") String employeeId);

    // ✅ ALTERNATIVE: Find project by ID and check if employee is assigned
    @Query("SELECT p FROM Project p WHERE p.id = :projectId AND :employeeId MEMBER OF p.assignedEmployeeIds")
    Optional<Project> findProjectIfEmployeeAssigned(@Param("projectId") Long projectId, @Param("employeeId") String employeeId);

    // Simple find all projects for a given clientId (no pagination)

    List<Project> findByClientId(String clientId);

    long countByClientId(String clientId);
    @Query("select coalesce(sum(p.budget),0) from Project p where p.clientId = :clientId")
    BigDecimal sumBudgetByClientId(@Param("clientId") String clientId);

    // Count projects whose status is in the given collection
    long countByProjectStatusIn(Collection<ProjectStatus> statuses);

    // Count projects whose status is in given collection AND deadline before given date AND noDeadline = false
    long countByProjectStatusInAndDeadlineBeforeAndNoDeadlineFalse(Collection<ProjectStatus> statuses, LocalDate date);

    // --- New: counts scoped to a specific employee (works with ElementCollection) ---
    @Query("SELECT COUNT(p) FROM Project p JOIN p.assignedEmployeeIds ae " +
            "WHERE ae = :employeeId AND p.projectStatus IN :statuses")
    long countByEmployeeAndStatuses(@Param("employeeId") String employeeId,
                                    @Param("statuses") Collection<ProjectStatus> statuses);

    @Query("SELECT COUNT(p) FROM Project p JOIN p.assignedEmployeeIds ae " +
            "WHERE ae = :employeeId AND p.projectStatus IN :statuses AND p.noDeadline = false AND p.deadline < :date")
    long countOverdueByEmployeeAndStatuses(@Param("employeeId") String employeeId,
                                           @Param("statuses") Collection<ProjectStatus> statuses,
                                           @Param("date") LocalDate date);


    @Query("SELECT t.projectId FROM Task t WHERE t.id = :taskId")
    Long findProjectIdById(@Param("taskId") Long taskId);

    @Query("SELECT p.shortCode FROM Project p WHERE p.id = :projectId")
    String findShortCodeById(@Param("projectId") Long projectId);
}
