package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.timesheet.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TimeLogService {
    TimeLogDto create(TimeLogCreateRequest req, String actor);
    TimeLogDto update(Long id, TimeLogCreateRequest req, String actor);
    void delete(Long id, String actor);
    List<TimeLogDto> listByEmployee(String employeeId);

    @Transactional(readOnly = true)
    List<EmployeeTimesheetSummaryDto> getAllEmployeesTimesheetSummary();

    @Transactional(readOnly = true)
    EmployeeTimesheetSummaryDto getMyTimesheetSummary(String employeeId);

    Long sumDurationMinutesByProject(Long projectId);
    List<TimeLogDto> listAll();

    List<TimeLogDto> listByProject(Long projectId, String actor);

    EmployeeTimeLogHoursDto getTotalHoursForEmployee(String employeeId);

    // New:
    TimeLogDayResponse getTimeLogsForEmployeeOnDate(String employeeId, java.time.LocalDate date);
    java.util.List<TimeLogDaySummaryDto> getWeekSummaryForEmployee(String employeeId, java.time.LocalDate weekStartDate);

    // Add method to interface
    List<TimeLogDto> listByTask(Long taskId, String actor);


    // ✅ NEW
    WeeklyTimeLogCreateResponse createWeekly(String actor, WeeklyTimeLogCreateRequest request);

}
