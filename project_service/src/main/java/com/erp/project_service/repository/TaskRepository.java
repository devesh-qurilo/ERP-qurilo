package com.erp.project_service.repository;

import com.erp.project_service.dto.task.TaskDto;
import com.erp.project_service.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    @Query("select t from Task t join t.assignedEmployeeIds a where a = :employeeId")
    Page<Task> findByAssignedEmployeeId(@Param("employeeId") String employeeId, Pageable pageable);

    // find tasks waiting for approval
    @Query("select t from Task t where t.statusEnum = 'WAITING'")
    Page<Task> findWaitingTasks(Pageable pageable);

    // find tasks by dependentTaskId
    List<Task> findByDependentTaskId(Long dependentTaskId);

    // ✅ FIX 2: For single task fetch with labels
    @EntityGraph(attributePaths = {"labels"})
    Optional<Task> findById(Long id);

    // ✅ FIX 3: Alternative using JOIN FETCH
    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.labels WHERE t.id = :id")
    Optional<Task> findByIdWithLabels(@Param("id") Long id);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.labels WHERE t.projectId = :projectId")
    List<Task> findByProjectIdWithLabels(@Param("projectId") Long projectId);

    Long getProjectById(Long projectId);

    // ✅ Find by taskStage name
    @Query("SELECT t FROM Task t WHERE t.taskStage.name = :statusName")
    List<Task> findByStatusName(@Param("statusName") String statusName);

    // ✅ Find waiting tasks (using statusEnum as fallback)
    @Query("SELECT t FROM Task t WHERE t.statusEnum = 'WAITING'")
    List<Task> findAllWaitingTasks();

    // ✅ Find by taskStage
    List<Task> findByTaskStageId(Long taskStageId);

    // ✅ Find by project and taskStage
    List<Task> findByProjectIdAndTaskStageId(Long projectId, Long taskStageId);

    // ✅ For backward compatibility - using @Query to handle the new structure
    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.taskStage.id = :statusId")
    Page<Task> findByProjectIdAndStatusId(@Param("projectId") Long projectId,
                                          @Param("statusId") Long statusId,
                                          Pageable pageable);

    // ✅ FIXED: Check if employee is assigned to task using ElementCollection
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Task t WHERE t.id = :taskId AND :employeeId MEMBER OF t.assignedEmployeeIds")
    boolean isEmployeeAssignedToTask(@Param("employeeId") String employeeId, @Param("taskId") Long taskId);

    boolean existsByProjectIdAndTitleAndStartDate(Long projectId, String title, LocalDate candidate);

    // --- Global counts (admin) ---
    @Query("SELECT COUNT(t) FROM Task t " +
            "WHERE (t.taskStage IS NULL OR LOWER(t.taskStage.name) NOT IN :completedNames)")
    long countPendingTasks(@Param("completedNames") java.util.List<String> completedNames);

    @Query("SELECT COUNT(t) FROM Task t " +
            "WHERE (t.taskStage IS NULL OR LOWER(t.taskStage.name) NOT IN :completedNames) " +
            "AND t.noDueDate = false AND t.dueDate < :date")
    long countOverdueTasks(@Param("completedNames") java.util.List<String> completedNames,
                           @Param("date") LocalDate date);

    // --- Employee-scoped counts (join ElementCollection) ---
    @Query("SELECT COUNT(t) FROM Task t JOIN t.assignedEmployeeIds ae " +
            "WHERE ae = :employeeId AND (t.taskStage IS NULL OR LOWER(t.taskStage.name) NOT IN :completedNames)")
    long countPendingByEmployee(@Param("employeeId") String employeeId,
                                @Param("completedNames") java.util.List<String> completedNames);

    @Query("SELECT COUNT(t) FROM Task t JOIN t.assignedEmployeeIds ae " +
            "WHERE ae = :employeeId " +
            "AND (t.taskStage IS NULL OR LOWER(t.taskStage.name) NOT IN :completedNames) " +
            "AND t.noDueDate = false AND t.dueDate < :date")
    long countOverdueByEmployee(@Param("employeeId") String employeeId,
                                @Param("completedNames") java.util.List<String> completedNames,
                                @Param("date") LocalDate date);



}
