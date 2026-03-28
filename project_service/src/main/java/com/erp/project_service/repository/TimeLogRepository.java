package com.erp.project_service.repository;

import com.erp.project_service.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {
    List<TimeLog> findByEmployeeId(String employeeId);
    List<TimeLog> findByProjectId(Long projectId);
    List<TimeLog> findByTaskId(Long taskId);

    @Query("select sum(t.durationMinutes) from TimeLog t where t.projectId = :projectId")
    Long sumDurationMinutesByProjectId(Long projectId);

    // Example: in TimeLogRepository (interface extends JpaRepository/CrudRepository)
    @Query("select coalesce(sum(t.durationMinutes), 0) from TimeLog t where t.employeeId = :employeeId")
    Long sumDurationMinutesByEmployeeId(@Param("employeeId") String employeeId);

    // Find timelogs where the given date falls between startDate and endDate (inclusive).
    @Query("select t from TimeLog t where t.employeeId = :employeeId and :date between t.startDate and coalesce(t.endDate, t.startDate)")
    List<TimeLog> findByEmployeeIdAndCoversDate(@Param("employeeId") String employeeId, @Param("date") java.time.LocalDate date);

    // Find timelogs overlapping date range [from .. to]
    @Query("select t from TimeLog t where t.employeeId = :employeeId and t.startDate <= :to and coalesce(t.endDate, t.startDate) >= :from")
    List<TimeLog> findByEmployeeIdAndOverlappingRange(@Param("employeeId") String employeeId,
                                                      @Param("from") java.time.LocalDate from,
                                                      @Param("to") java.time.LocalDate to);

    // returns total duration minutes for a task (or null if none)
    @Query("select sum(t.durationMinutes) from TimeLog t where t.taskId = :taskId")
    Long sumDurationMinutesByTaskId(@Param("taskId") Long taskId);

    // ✅ NEW: ek employee + task + date pe sirf ek timelog allow
    boolean existsByEmployeeIdAndTaskIdAndStartDate(String employeeId, Long taskId, LocalDate startDate);


    @Query("SELECT DISTINCT t.employeeId FROM TimeLog t ORDER BY t.employeeId")
    Set<String> findAllDistinctEmployeeIds();
}
