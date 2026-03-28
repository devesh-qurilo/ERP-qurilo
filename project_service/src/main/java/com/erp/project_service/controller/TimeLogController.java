package com.erp.project_service.controller;

import com.erp.project_service.dto.timesheet.*;
import com.erp.project_service.service.interfaces.TimeLogService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/timesheets")
@RequiredArgsConstructor
public class TimeLogController {

    private final TimeLogService svc;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping
    public ResponseEntity<TimeLogDto> create(@RequestBody TimeLogCreateRequest req) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(201).body(svc.create(req, actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PutMapping("/{id}")
    public ResponseEntity<TimeLogDto> update(@PathVariable Long id, @RequestBody TimeLogCreateRequest req) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.update(id, req, actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        svc.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/me")
    public ResponseEntity<List<TimeLogDto>> myLogs() {
        String me = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.listByEmployee(me));
    }

    // NEW ENDPOINT: Only admin can see all timelogs
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<List<TimeLogDto>> getAllTimeLogs() {
        return ResponseEntity.ok(svc.listAll());
    }

    // NEW ENDPOINT: Get time logs by project (Admin or assigned employees only)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TimeLogDto>> getTimeLogsByProject(@PathVariable Long projectId) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.listByProject(projectId, actor));
    }


    /**
     * List timelogs for an employee
     * GET /api/timelogs/employee/{employeeId}
     *
     * - Admin can view any employee's timelogs.
     * - Employee can view their own timelogs (service enforces).
     */
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> listByEmployee(@PathVariable String employeeId) {
        try {
            List<TimeLogDto> list = svc.listByEmployee(employeeId);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(403).body("Failed to fetch timelogs: " + e.getMessage());
        }
    }

    @GetMapping("/employee/{employeeId}/hours")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTotalHoursForEmployee(@PathVariable String employeeId) {
        try {
            EmployeeTimeLogHoursDto dto = svc.getTotalHoursForEmployee(employeeId);
            // If you want to restrict employees to see only their own hours (already enforced in service if desired),
            // you may add an authorization check in the service.
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to compute total hours: " + e.getMessage());
        }
    }

    /**
     * GET timelogs & summary for current user on a single date.
     * Example: GET /timesheets/me/day?date=2025-11-10
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/me/day")
    public ResponseEntity<TimeLogDayResponse> myLogsForDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String me = SecurityUtils.getCurrentUserId();
        TimeLogDayResponse resp = svc.getTimeLogsForEmployeeOnDate(me, date);
        return ResponseEntity.ok(resp);
    }

    /**
     * GET week summary (7 days) starting from provided date (e.g., monday).
     * Example: GET /timesheets/me/week?startDate=2025-11-10
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/me/week")
    public ResponseEntity<List<TimeLogDaySummaryDto>> myWeekSummary(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        String me = SecurityUtils.getCurrentUserId();
        List<TimeLogDaySummaryDto> week = svc.getWeekSummaryForEmployee(me, startDate);
        return ResponseEntity.ok(week);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TimeLogDto>> getTimeLogsByTask(@PathVariable Long taskId) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.listByTask(taskId, actor));
    }

    /**
     * Weekly timelog create:
     * - Body: WeeklyTimeLogCreateRequest
     * - For each filled date -> new timelog
     * - Existing date (same employee+task+date) -> skip, response me alreadyFilled
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping("/weekly")
    public ResponseEntity<WeeklyTimeLogCreateResponse> createWeekly(
            @RequestBody WeeklyTimeLogCreateRequest req) {

        String actor = SecurityUtils.getCurrentUserId();
        WeeklyTimeLogCreateResponse resp = svc.createWeekly(actor, req);
        return ResponseEntity.status(201).body(resp);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<EmployeeTimesheetSummaryDto>> getAllEmployeesTimesheets() {

        List<EmployeeTimesheetSummaryDto> timesheets;
        timesheets = svc.getAllEmployeesTimesheetSummary();

        return ResponseEntity.ok(timesheets);
    }

    /**
     * Employee → Own timesheet summary
     * GET /timesheets/me/summary
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/me/summary")
    public ResponseEntity<EmployeeTimesheetSummaryDto> myTimesheetSummary() {
        String me = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.getMyTimesheetSummary(me));
    }
}
