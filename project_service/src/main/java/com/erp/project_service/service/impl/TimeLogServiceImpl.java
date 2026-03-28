//package com.erp.project_service.service.impl;
//
//import com.erp.project_service.client.EmployeeClient;
//import com.erp.project_service.dto.common.EmployeeMetaDto;
//import com.erp.project_service.dto.timesheet.*;
//import com.erp.project_service.entity.TimeLog;
//import com.erp.project_service.exception.AccessDeniedException;
//import com.erp.project_service.exception.NotFoundException;
//import com.erp.project_service.mapper.TimeLogMapper;
//import com.erp.project_service.repository.ProjectRepository;
//import com.erp.project_service.repository.TaskRepository;
//import com.erp.project_service.repository.TimeLogRepository;
//import com.erp.project_service.service.interfaces.ProjectActivityService;
//import com.erp.project_service.service.interfaces.TimeLogService;
//import com.erp.project_service.service.notification.NotificationHelperService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.Duration;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class TimeLogServiceImpl implements TimeLogService {
//
//    private final TimeLogRepository repo;
//    private final ProjectActivityService activityService;
//    private final ProjectRepository projectRepository;
//    private final TaskRepository taskRepository;
//    private final EmployeeClient employeeClient; // NEW
//    private final NotificationHelperService notificationHelper; // NEW
//
//
//    @Override
//    @Transactional
//    public TimeLogDto create(TimeLogCreateRequest req, String actor) {
//        // Authorization check: Employee can only create their own timelogs
//        if (!hasAdminRole() && !actor.equals(req.getEmployeeId())) {
//            throw new AccessDeniedException("You can only create your own timelogs");
//        }
//
//        // Validation: Check if employee belongs to task/project
//        validateEmployeeAssignment(req.getEmployeeId(), req.getProjectId(), req.getTaskId());
//
//        TimeLog t = TimeLogMapper.toEntity(req);
//        t.setCreatedBy(actor);
//        computeDuration(t);
//        TimeLog saved = repo.save(t);
//
//        // NEW: Send custom time log notification
//        sendTimeLogNotification(actor, req.getEmployeeId(), req.getProjectId(), saved.getDurationMinutes(), "CREATED");
//
//        activityService.record(t.getProjectId(), actor, "TIMELOG_CREATED", String.valueOf(saved.getId()));
//
//        // Convert to DTO with employee details
//        return toDtoWithEmployeeDetails(saved);
//    }
//
//    @Override
//    @Transactional
//    public TimeLogDto update(Long id, TimeLogCreateRequest req, String actor) {
//        TimeLog t = repo.findById(id).orElseThrow(() -> new NotFoundException("TimeLog not found"));
//
//        // Authorization check: Employee can only update their own timelogs
//        if (!hasAdminRole() && !actor.equals(t.getEmployeeId())) {
//            throw new AccessDeniedException("You can only update your own timelogs");
//        }
//
//        // Determine which employeeId to validate
//        String employeeIdToValidate;
//        if (hasAdminRole() && req.getEmployeeId() != null) {
//            // Admin is changing employeeId, validate the NEW employee
//            employeeIdToValidate = req.getEmployeeId();
//        } else {
//            // Employee updating their own OR admin not changing employeeId
//            employeeIdToValidate = t.getEmployeeId();
//        }
//
//        // Validation: Check if employee belongs to task/project
//        validateEmployeeAssignment(employeeIdToValidate, req.getProjectId(), req.getTaskId());
//
//        // Apply updates
//        if (req.getProjectId() != null) t.setProjectId(req.getProjectId());
//        if (req.getTaskId() != null) t.setTaskId(req.getTaskId());
//        if (req.getStartDate() != null) t.setStartDate(req.getStartDate());
//        if (req.getStartTime() != null) t.setStartTime(req.getStartTime());
//        if (req.getEndDate() != null) t.setEndDate(req.getEndDate());
//        if (req.getEndTime() != null) t.setEndTime(req.getEndTime());
//        if (req.getMemo() != null) t.setMemo(req.getMemo());
//
//        // If admin is updating someone else's timelog, allow employeeId change
//        if (hasAdminRole() && req.getEmployeeId() != null && !req.getEmployeeId().equals(t.getEmployeeId())) {
//            t.setEmployeeId(req.getEmployeeId());
//        }
//
//        t.setUpdatedBy(actor);
//        computeDuration(t);
//        TimeLog saved = repo.save(t);
//
//        // NEW: Send time log update notification
//        sendTimeLogNotification(actor, t.getEmployeeId(), t.getProjectId(), saved.getDurationMinutes(), "UPDATED");
//
//        activityService.record(t.getProjectId(), actor, "TIMELOG_UPDATED", String.valueOf(saved.getId()));
//
//        // Convert to DTO with employee details
//        return toDtoWithEmployeeDetails(saved);
//    }
//
//    @Override
//    @Transactional
//    public void delete(Long id, String actor) {
//        TimeLog t = repo.findById(id).orElseThrow(() -> new NotFoundException("TimeLog not found"));
//
//        // Authorization check: Employee can only delete their own timelogs
//        if (!hasAdminRole() && !actor.equals(t.getEmployeeId())) {
//            throw new AccessDeniedException("You can only delete your own timelogs");
//        }
//
//        repo.deleteById(id);
//
//        // NEW: Send time log deletion notification
//        sendTimeLogNotification(actor, t.getEmployeeId(), t.getProjectId(), t.getDurationMinutes(), "DELETED");
//
//        activityService.record(t.getProjectId(), actor, "TIMELOG_DELETED", String.valueOf(id));
//    }
//
//
//    //Notifiction helper
//    // NEW METHOD: Send time log notification
//    private void sendTimeLogNotification(String actor, String employeeId, Long projectId, Long durationMinutes, String action) {
//        try {
//            String title = "";
//            String message = "";
//            Long durationHours = durationMinutes != null ? durationMinutes / 60 : 0;
//
//            if ("CREATED".equals(action)) {
//                title = "⏱️ Time Log Created";
//                message = String.format(
//                        "Time log has been successfully created. " +
//                                "Project ID: %d | Duration: %d hours (%d minutes). " +
//                                "The entry has been recorded in your timesheet.",
//                        projectId, durationHours, durationMinutes
//                );
//            } else if ("UPDATED".equals(action)) {
//                title = "✏️ Time Log Updated";
//                message = String.format(
//                        "Your time log has been updated. " +
//                                "Project ID: %d | Duration: %d hours (%d minutes). " +
//                                "Changes have been saved successfully.",
//                        projectId, durationHours, durationMinutes
//                );
//            } else if ("DELETED".equals(action)) {
//                title = "🗑️ Time Log Deleted";
//                message = String.format(
//                        "Time log has been deleted. " +
//                                "Project ID: %d | Duration: %d hours (%d minutes). " +
//                                "The entry has been removed from your timesheet.",
//                        projectId, durationHours, durationMinutes
//                );
//            }
//
//            notificationHelper.sendNotification(actor, employeeId, title, message, "TIME_LOG");
//
//            log.info("Time log {} notification sent to {}", action, employeeId);
//
//        } catch (Exception e) {
//            log.error("Failed to send time log {} notification: {}", action, e.getMessage());
//        }
//    }
//
//
//
//    @Override
//    public List<TimeLogDto> listByEmployee(String employeeId) {
//        // Authorization check: Employee can only view their own timelogs
//        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (!hasAdminRole() && !currentUser.equals(employeeId)) {
//            throw new AccessDeniedException("You can only view your own timelogs");
//        }
//
//        List<TimeLog> timeLogs = repo.findByEmployeeId(employeeId);
//        return toDtoListWithEmployeeDetails(timeLogs);
//    }
//
//    @Override
//    public List<TimeLogDto> listAll() {
//        List<TimeLog> timeLogs = repo.findAll();
//        return toDtoListWithEmployeeDetails(timeLogs);
//    }
//
//    @Override
//    public List<TimeLogDto> listByProject(Long projectId, String actor) {
//        // Validate project exists and user has access
//        validateUserProjectAccess(actor, projectId);
//
//        // Get all time logs for the project
//        List<TimeLog> timeLogs = repo.findByProjectId(projectId);
//
//        // Convert to DTOs with employee details
//        return toDtoListWithEmployeeDetails(timeLogs);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public EmployeeTimeLogHoursDto getTotalHoursForEmployee(String employeeId) {
//        if (employeeId == null || employeeId.trim().isEmpty()) {
//            return EmployeeTimeLogHoursDto.builder()
//                    .employeeId(employeeId)
//                    .totalMinutes(0L)
//                    .totalHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN))
//                    .build();
//        }
//
//        Long totalMinutes = null;
//        try {
//            // Preferred: use repository aggregation if available
//            totalMinutes = repo.sumDurationMinutesByEmployeeId(employeeId);
//        } catch (NoSuchMethodError | UnsupportedOperationException ex) {
//            // repository method not available or failed — fallback to scanning in-memory
//            log.warn("Repository sum method not found or failed: {}. Falling back to in-memory sum.", ex.getMessage());
//        } catch (Exception ex) {
//            log.warn("Failed to compute sum from repo: {}. Falling back to in-memory sum.", ex.getMessage());
//        }
//
//        if (totalMinutes == null) {
//            // Fallback: compute from loaded entities (least efficient)
//            List<TimeLog> timeLogs = repo.findByEmployeeId(employeeId);
//            totalMinutes = timeLogs == null ? 0L :
//                    timeLogs.stream()
//                            .map(TimeLog::getDurationMinutes)
//                            .filter(Objects::nonNull)
//                            .mapToLong(Long::longValue)
//                            .sum();
//        }
//
//        if (totalMinutes == null) totalMinutes = 0L;
//
//        BigDecimal totalHours = BigDecimal.valueOf(totalMinutes)
//                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_EVEN);
//
//        return EmployeeTimeLogHoursDto.builder()
//                .employeeId(employeeId)
//                .totalMinutes(totalMinutes)
//                .totalHours(totalHours)
//                .build();
//    }
//    @Override
//    public TimeLogDayResponse getTimeLogsForEmployeeOnDate(String employeeId, LocalDate date) {
//        if (employeeId == null || date == null) throw new IllegalArgumentException("employeeId and date are required");
//
//        // authorize: employee can fetch their own or admin can fetch others
//        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (!hasAdminRole() && !currentUser.equals(employeeId)) {
//            throw new AccessDeniedException("You can only view your own timelogs");
//        }
//
//        // Fetch time logs that cover this date
//        List<TimeLog> logs = repo.findByEmployeeIdAndCoversDate(employeeId, date);
//
//        // Convert entity list -> DTO list (with employee details)
//        List<TimeLogDto> timeLogDtos = toDtoListWithEmployeeDetails(logs);
//
//        // For summary: compute overlapping minutes of each timelog for this date
//        Map<Long, Long> minutesPerProject = new HashMap<>(); // projectId -> minutes
//        long totalMinutes = 0L;
//
//        for (TimeLog t : logs) {
//            // compute effective start datetime for this date
//            LocalDate logStartDate = t.getStartDate();
//            LocalDate logEndDate = (t.getEndDate() == null) ? t.getStartDate() : t.getEndDate();
//
//            LocalDate effectiveStartDate = logStartDate.isAfter(date) ? logStartDate : date;
//            LocalDate effectiveEndDate = logEndDate.isBefore(date) ? logEndDate : date;
//
//            // compute start time & end time for the clipped day
//            LocalTime startTime = t.getStartTime() != null ? t.getStartTime() : LocalTime.MIN;
//            LocalTime endTime = t.getEndTime() != null ? t.getEndTime() : LocalTime.MAX;
//
//            // If the timelog spans multiple days and startDate < date, then for this day start at 00:00
//            LocalDateTime startDateTime = (t.getStartDate().isBefore(date)) ? LocalDateTime.of(date, LocalTime.MIN)
//                    : LocalDateTime.of(t.getStartDate(), startTime);
//
//            // If the timelog ends after this day, clip to end of day
//            LocalDateTime endDateTime = ( (t.getEndDate() == null ? t.getStartDate() : t.getEndDate()).isAfter(date) )
//                    ? LocalDateTime.of(date, LocalTime.MAX)
//                    : LocalDateTime.of(t.getEndDate(), endTime);
//
//            // Defensive: ensure start <= end
//            if (endDateTime.isBefore(startDateTime)) {
//                continue;
//            }
//
//            long minutes = ChronoUnit.MINUTES.between(startDateTime, endDateTime);
//            if (minutes < 0) minutes = 0;
//
//            totalMinutes += minutes;
//
//            Long projId = t.getProjectId();
//            if (projId == null) projId = -1L;
//            minutesPerProject.merge(projId, minutes, Long::sum);
//        }
//
//        // build segment list
//        List<TimeLogSegmentDto> segments = minutesPerProject.entrySet().stream()
//                .map(e -> {
//                    Long projId = e.getKey();
//                    Long mins = e.getValue();
//                    BigDecimal hrs = BigDecimal.valueOf(mins).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_EVEN);
//
//                    // Optionally fetch project name/color if you have projectRepository
//                    String projectName = null;
//                    String color = null;
//                    try {
//                        if (projId != -1L && projectRepository.existsById(projId)) {
//                            var p = projectRepository.findById(projId).orElse(null);
//                            if (p != null) {
//                                projectName = p.getName();
//                                // If your project has color code field, set color here
//                                // color = p.getColorCode();
//                            }
//                        }
//                    } catch (Exception ignored) {}
//
//                    return TimeLogSegmentDto.builder()
//                            .projectId(projId == -1L ? null : projId)
//                            .projectName(projectName)
//                            .minutes(mins)
//                            .hours(hrs)
//                            .color(color)
//                            .build();
//                })
//                .collect(Collectors.toList());
//
//        BigDecimal totalHours = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_EVEN);
//
//        TimeLogDaySummaryDto summary = TimeLogDaySummaryDto.builder()
//                .date(date)
//                .totalMinutes(totalMinutes)
//                .totalHours(totalHours)
//                .segments(segments)
//                .build();
//
//        return TimeLogDayResponse.builder()
//                .timeLogs(timeLogDtos)
//                .summary(summary)
//                .build();
//    }
//
//    @Override
//    public List<TimeLogDaySummaryDto> getWeekSummaryForEmployee(String employeeId, LocalDate weekStartDate) {
//        if (employeeId == null || weekStartDate == null) throw new IllegalArgumentException("employeeId and weekStartDate required");
//
//        // authorize
//        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
//        if (!hasAdminRole() && !currentUser.equals(employeeId)) {
//            throw new AccessDeniedException("You can only view your own timelogs");
//        }
//
//        List<TimeLogDaySummaryDto> week = new ArrayList<>();
//        LocalDate cur = weekStartDate;
//        for (int i = 0; i < 7; i++) {
//            // For each day compute summary using the same logic (could call previous method)
//            TimeLogDayResponse dayResp = getTimeLogsForEmployeeOnDate(employeeId, cur);
//            week.add(dayResp.getSummary());
//            cur = cur.plusDays(1);
//        }
//        return week;
//    }
//
//    @Override
//    public List<TimeLogDto> listByTask(Long taskId, String actor) {
//        if (taskId == null) {
//            throw new IllegalArgumentException("Task ID is required");
//        }
//
//        // Admin can access all
//        if (!hasAdminRole()) {
//            // Non-admin: ensure actor is assigned to this task
//            boolean assigned = false;
//            try {
//                assigned = taskRepository.isEmployeeAssignedToTask(actor, taskId);
//            } catch (Exception e) {
//                log.warn("Failed to check task assignment for {} on task {}: {}", actor, taskId, e.getMessage());
//            }
//            if (!assigned) {
//                throw new AccessDeniedException("You are not assigned to task " + taskId);
//            }
//        }
//
//        // Fetch timelogs
//        List<TimeLog> timeLogs = repo.findByTaskId(taskId);
//        return toDtoListWithEmployeeDetails(timeLogs);
//    }
//
//
//    // NEW METHOD: Validate user has access to project
//    private void validateUserProjectAccess(String userId, Long projectId) {
//        if (projectId == null) {
//            throw new AccessDeniedException("Project ID is required");
//        }
//
//        // Check if user is admin
//        if (hasAdminRole()) {
//            return; // Admin has access to all projects
//        }
//
//        // Check if user is assigned to the project
//        boolean isAssignedToProject = projectRepository.isEmployeeAssignedToProject(projectId, userId);
//        if (!isAssignedToProject) {
//            throw new AccessDeniedException("You are not assigned to project " + projectId);
//        }
//    }
//
//    @Override
//    public Long sumDurationMinutesByProject(Long projectId) {
//        Long s = repo.sumDurationMinutesByProjectId(projectId);
//        return s == null ? 0L : s;
//    }
//
//    // NEW METHOD: Convert single TimeLog to DTO with employee details
//    private TimeLogDto toDtoWithEmployeeDetails(TimeLog timeLog) {
//        TimeLogDto dto = TimeLogMapper.toDto(timeLog);
//
//        // Fetch employee details
//        try {
//            EmployeeMetaDto employeeDetail = employeeClient.getMeta(timeLog.getEmployeeId());
//            if (employeeDetail != null) {
//                dto.setEmployees(List.of(employeeDetail));
//            }
//        } catch (Exception e) {
//            log.warn("Failed to fetch employee details for ID: {}", timeLog.getEmployeeId(), e);
//            // Continue without employee details
//        }
//
//        return dto;
//    }
//
//    // NEW METHOD: Convert list of TimeLogs to DTOs with employee details
//    private List<TimeLogDto> toDtoListWithEmployeeDetails(List<TimeLog> timeLogs) {
//        if (timeLogs == null || timeLogs.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        // Extract unique employee IDs
//        Set<String> employeeIds = timeLogs.stream()
//                .map(TimeLog::getEmployeeId)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toSet());
//
//        // Fetch employee details and create a map
//        Map<String, EmployeeMetaDto> employeeMap = fetchEmployeeDetailsMap(employeeIds);
//
//        // Convert to DTOs
//        return timeLogs.stream().map(timeLog -> {
//            TimeLogDto dto = TimeLogMapper.toDto(timeLog);
//
//            // Set employee details
//            EmployeeMetaDto employeeDetail = employeeMap.get(timeLog.getEmployeeId());
//            if (employeeDetail != null) {
//                dto.setEmployees(List.of(employeeDetail));
//            }
//
//            return dto;
//        }).collect(Collectors.toList());
//    }
//
//    // NEW METHOD: Fetch employee details and return as Map
//    private Map<String, EmployeeMetaDto> fetchEmployeeDetailsMap(Set<String> employeeIds) {
//        Map<String, EmployeeMetaDto> employeeMap = new HashMap<>();
//
//        if (employeeIds == null || employeeIds.isEmpty()) {
//            return employeeMap;
//        }
//
//        // Fetch each employee detail individually
//        for (String employeeId : employeeIds) {
//            try {
//                EmployeeMetaDto employeeDetail = employeeClient.getMeta(employeeId);
//                if (employeeDetail != null) {
//                    employeeMap.put(employeeId, employeeDetail);
//                }
//            } catch (Exception e) {
//                log.warn("Failed to fetch employee details for ID: {}", employeeId, e);
//                // Continue with other employees
//            }
//        }
//
//        return employeeMap;
//    }
//
//    private void validateEmployeeAssignment(String employeeId, Long projectId, Long taskId) {
//        // If taskId is provided, check task assignment
//        if (taskId != null) {
//            boolean isAssignedToTask = taskRepository.isEmployeeAssignedToTask(employeeId, taskId);
//            if (!isAssignedToTask) {
//                throw new AccessDeniedException("Employee " + employeeId + " is not assigned to task " + taskId);
//            }
//        }
//
//        // If projectId is provided, check project assignment
//        if (projectId != null) {
//            boolean isAssignedToProject = projectRepository.isEmployeeAssignedToProject(projectId, employeeId);
//            if (!isAssignedToProject) {
//                throw new AccessDeniedException("Employee " + employeeId + " is not assigned to project " + projectId);
//            }
//        }
//
//        // If both are null, it's okay (general timelog without specific project/task)
//    }
//
////    private void computeDuration(TimeLog t) {
////        if (t.getStartDate() != null && t.getEndDate() != null && t.getStartTime() != null && t.getEndTime() != null) {
////            try {
////                LocalDate sd = t.getStartDate();
////                LocalDate ed = t.getEndDate();
////                LocalTime st = t.getStartTime();
////                LocalTime et = t.getEndTime();
////                long minutes = Duration.between(sd.atTime(st), ed.atTime(et)).toHours();
////                t.setDurationMinutes(Math.max(0L, minutes));
////            } catch (Exception ex) {
////                t.setDurationMinutes(0L);
////            }
////        } else {
////            t.setDurationMinutes(0L);
////        }
////    }
//
//    private void computeDuration(TimeLog t) {
//        if (t.getStartDate() != null && t.getEndDate() != null && t.getStartTime() != null && t.getEndTime() != null) {
//            try {
//                LocalDateTime startDateTime = LocalDateTime.of(t.getStartDate(), t.getStartTime());
//                LocalDateTime endDateTime = LocalDateTime.of(t.getEndDate(), t.getEndTime());
//
//                long minutes = Duration.between(startDateTime, endDateTime).toMinutes();
//                t.setDurationMinutes(Math.max(0L, minutes));
//
//            } catch (Exception ex) {
//                log.error("Error computing duration for timelog: {}", t.getId(), ex);
//                t.setDurationMinutes(0L);
//            }
//        } else {
//            t.setDurationMinutes(0L);
//        }
//    }
//
//    // Helper method to check if current user has admin role
//    private boolean hasAdminRole() {
//        return SecurityContextHolder.getContext().getAuthentication()
//                .getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .anyMatch(role -> role.equals("ROLE_ADMIN"));
//    }
//}


package com.erp.project_service.service.impl;

import com.erp.project_service.client.EmployeeClient;
import com.erp.project_service.dto.common.EmployeeMetaDto;
import com.erp.project_service.dto.timesheet.*;
import com.erp.project_service.entity.Task;
import com.erp.project_service.entity.TimeLog;
import com.erp.project_service.exception.AccessDeniedException;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.mapper.TimeLogMapper;
import com.erp.project_service.repository.ProjectRepository;
import com.erp.project_service.repository.TaskRepository;
import com.erp.project_service.repository.TimeLogRepository;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import com.erp.project_service.service.interfaces.TimeLogService;
import com.erp.project_service.service.notification.NotificationHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeLogServiceImpl implements TimeLogService {

    private final TimeLogRepository repo;
    private final ProjectActivityService activityService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final EmployeeClient employeeClient;
    private final NotificationHelperService notificationHelper;

    @Override
    @Transactional
    public TimeLogDto create(TimeLogCreateRequest req, String actor) {
        // Authorization check: Employee can only create their own timelogs
        if (!hasAdminRole() && !actor.equals(req.getEmployeeId())) {
            throw new AccessDeniedException("You can only create your own timelogs");
        }

        // Validation: Check if employee belongs to task/project
        validateEmployeeAssignment(req.getEmployeeId(), req.getProjectId(), req.getTaskId());

        TimeLog t = TimeLogMapper.toEntity(req);
        t.setCreatedBy(actor);
        computeDuration(t);
        TimeLog saved = repo.save(t);

        // NEW: Send custom time log notification
        sendTimeLogNotification(actor, req.getEmployeeId(), req.getProjectId(), saved.getDurationMinutes(), "CREATED");

        activityService.record(t.getProjectId(), actor, "TIMELOG_CREATED", String.valueOf(saved.getId()));

        // Convert to DTO with employee details AND project shortcode
        return toDtoWithEmployeeAndProjectDetails(saved);
    }

    @Override
    @Transactional
    public TimeLogDto update(Long id, TimeLogCreateRequest req, String actor) {
        TimeLog t = repo.findById(id).orElseThrow(() -> new NotFoundException("TimeLog not found"));

        // Authorization check: Employee can only update their own timelogs
        if (!hasAdminRole() && !actor.equals(t.getEmployeeId())) {
            throw new AccessDeniedException("You can only update your own timelogs");
        }

        // Determine which employeeId to validate
        String employeeIdToValidate;
        if (hasAdminRole() && req.getEmployeeId() != null) {
            // Admin is changing employeeId, validate the NEW employee
            employeeIdToValidate = req.getEmployeeId();
        } else {
            // Employee updating their own OR admin not changing employeeId
            employeeIdToValidate = t.getEmployeeId();
        }

        // Validation: Check if employee belongs to task/project
        validateEmployeeAssignment(employeeIdToValidate, req.getProjectId(), req.getTaskId());

        // Apply updates
        if (req.getProjectId() != null) t.setProjectId(req.getProjectId());
        if (req.getTaskId() != null) t.setTaskId(req.getTaskId());
        if (req.getStartDate() != null) t.setStartDate(req.getStartDate());
        if (req.getStartTime() != null) t.setStartTime(req.getStartTime());
        if (req.getEndDate() != null) t.setEndDate(req.getEndDate());
        if (req.getEndTime() != null) t.setEndTime(req.getEndTime());
        if (req.getMemo() != null) t.setMemo(req.getMemo());

        // If admin is updating someone else's timelog, allow employeeId change
        if (hasAdminRole() && req.getEmployeeId() != null && !req.getEmployeeId().equals(t.getEmployeeId())) {
            t.setEmployeeId(req.getEmployeeId());
        }

        t.setUpdatedBy(actor);
        computeDuration(t);
        TimeLog saved = repo.save(t);

        // NEW: Send time log update notification
        sendTimeLogNotification(actor, t.getEmployeeId(), t.getProjectId(), saved.getDurationMinutes(), "UPDATED");

        activityService.record(t.getProjectId(), actor, "TIMELOG_UPDATED", String.valueOf(saved.getId()));

        // Convert to DTO with employee details AND project shortcode
        return toDtoWithEmployeeAndProjectDetails(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, String actor) {
        TimeLog t = repo.findById(id).orElseThrow(() -> new NotFoundException("TimeLog not found"));

        // Authorization check: Employee can only delete their own timelogs
        if (!hasAdminRole() && !actor.equals(t.getEmployeeId())) {
            throw new AccessDeniedException("You can only delete your own timelogs");
        }

        repo.deleteById(id);

        // NEW: Send time log deletion notification
        sendTimeLogNotification(actor, t.getEmployeeId(), t.getProjectId(), t.getDurationMinutes(), "DELETED");

        activityService.record(t.getProjectId(), actor, "TIMELOG_DELETED", String.valueOf(id));
    }

    // Notification helper
    private void sendTimeLogNotification(String actor, String employeeId, Long projectId, Long durationMinutes, String action) {
        try {
            String title = "";
            String message = "";
            Long durationHours = durationMinutes != null ? durationMinutes / 60 : 0;

            if ("CREATED".equals(action)) {
                title = "⏱️ Time Log Created";
                message = String.format(
                        "Time log has been successfully created. " +
                                "Project ID: %d | Duration: %d hours (%d minutes). " +
                                "The entry has been recorded in your timesheet.",
                        projectId, durationHours, durationMinutes
                );
            } else if ("UPDATED".equals(action)) {
                title = "✏️ Time Log Updated";
                message = String.format(
                        "Your time log has been updated. " +
                                "Project ID: %d | Duration: %d hours (%d minutes). " +
                                "Changes have been saved successfully.",
                        projectId, durationHours, durationMinutes
                );
            } else if ("DELETED".equals(action)) {
                title = "🗑️ Time Log Deleted";
                message = String.format(
                        "Time log has been deleted. " +
                                "Project ID: %d | Duration: %d hours (%d minutes). " +
                                "The entry has been removed from your timesheet.",
                        projectId, durationHours, durationMinutes
                );
            }

            notificationHelper.sendNotification(actor, employeeId, title, message, "TIME_LOG");

            log.info("Time log {} notification sent to {}", action, employeeId);

        } catch (Exception e) {
            log.error("Failed to send time log {} notification: {}", action, e.getMessage());
        }
    }

    @Override
    public List<TimeLogDto> listByEmployee(String employeeId) {
        // Authorization check: Employee can only view their own timelogs
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!hasAdminRole() && !currentUser.equals(employeeId)) {
            throw new AccessDeniedException("You can only view your own timelogs");
        }

        List<TimeLog> timeLogs = repo.findByEmployeeId(employeeId);
        return toDtoListWithEmployeeAndProjectDetails(timeLogs);
    }

    @Override
    public List<TimeLogDto> listAll() {
        List<TimeLog> timeLogs = repo.findAll();
        return toDtoListWithEmployeeAndProjectDetails(timeLogs);
    }

    @Override
    public List<TimeLogDto> listByProject(Long projectId, String actor) {
        // Validate project exists and user has access
        validateUserProjectAccess(actor, projectId);

        // Get all time logs for the project
        List<TimeLog> timeLogs = repo.findByProjectId(projectId);

        // Convert to DTOs with employee details AND project shortcode
        return toDtoListWithEmployeeAndProjectDetails(timeLogs);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeTimeLogHoursDto getTotalHoursForEmployee(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return EmployeeTimeLogHoursDto.builder()
                    .employeeId(employeeId)
                    .totalMinutes(0L)
                    .totalHours(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN))
                    .build();
        }

        Long totalMinutes = null;
        try {
            // Preferred: use repository aggregation if available
            totalMinutes = repo.sumDurationMinutesByEmployeeId(employeeId);
        } catch (NoSuchMethodError | UnsupportedOperationException ex) {
            // repository method not available or failed — fallback to scanning in-memory
            log.warn("Repository sum method not found or failed: {}. Falling back to in-memory sum.", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Failed to compute sum from repo: {}. Falling back to in-memory sum.", ex.getMessage());
        }

        if (totalMinutes == null) {
            // Fallback: compute from loaded entities (least efficient)
            List<TimeLog> timeLogs = repo.findByEmployeeId(employeeId);
            totalMinutes = timeLogs == null ? 0L :
                    timeLogs.stream()
                            .map(TimeLog::getDurationMinutes)
                            .filter(Objects::nonNull)
                            .mapToLong(Long::longValue)
                            .sum();
        }

        if (totalMinutes == null) totalMinutes = 0L;

        BigDecimal totalHours = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_EVEN);

        return EmployeeTimeLogHoursDto.builder()
                .employeeId(employeeId)
                .totalMinutes(totalMinutes)
                .totalHours(totalHours)
                .build();
    }

    @Override
    public TimeLogDayResponse getTimeLogsForEmployeeOnDate(String employeeId, LocalDate date) {
        if (employeeId == null || date == null) throw new IllegalArgumentException("employeeId and date are required");

        // authorize: employee can fetch their own or admin can fetch others
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!hasAdminRole() && !currentUser.equals(employeeId)) {
            throw new AccessDeniedException("You can only view your own timelogs");
        }

        // Fetch time logs that cover this date
        List<TimeLog> logs = repo.findByEmployeeIdAndCoversDate(employeeId, date);

        // Convert entity list -> DTO list (with employee details AND project shortcode)
        List<TimeLogDto> timeLogDtos = toDtoListWithEmployeeAndProjectDetails(logs);

        // For summary: compute overlapping minutes of each timelog for this date
        Map<Long, Long> minutesPerProject = new HashMap<>(); // projectId -> minutes
        long totalMinutes = 0L;

        for (TimeLog t : logs) {
            // compute effective start datetime for this date
            LocalDate logStartDate = t.getStartDate();
            LocalDate logEndDate = (t.getEndDate() == null) ? t.getStartDate() : t.getEndDate();

            LocalDate effectiveStartDate = logStartDate.isAfter(date) ? logStartDate : date;
            LocalDate effectiveEndDate = logEndDate.isBefore(date) ? logEndDate : date;

            // compute start time & end time for the clipped day
            LocalTime startTime = t.getStartTime() != null ? t.getStartTime() : LocalTime.MIN;
            LocalTime endTime = t.getEndTime() != null ? t.getEndTime() : LocalTime.MAX;

            // If the timelog spans multiple days and startDate < date, then for this day start at 00:00
            LocalDateTime startDateTime = (t.getStartDate().isBefore(date)) ? LocalDateTime.of(date, LocalTime.MIN)
                    : LocalDateTime.of(t.getStartDate(), startTime);

            // If the timelog ends after this day, clip to end of day
            LocalDateTime endDateTime = ( (t.getEndDate() == null ? t.getStartDate() : t.getEndDate()).isAfter(date) )
                    ? LocalDateTime.of(date, LocalTime.MAX)
                    : LocalDateTime.of(t.getEndDate(), endTime);

            // Defensive: ensure start <= end
            if (endDateTime.isBefore(startDateTime)) {
                continue;
            }

            long minutes = ChronoUnit.MINUTES.between(startDateTime, endDateTime);
            if (minutes < 0) minutes = 0;

            totalMinutes += minutes;

            Long projId = t.getProjectId();
            if (projId == null) projId = -1L;
            minutesPerProject.merge(projId, minutes, Long::sum);
        }

        // Inside getTimeLogsForEmployeeOnDate method, update the segment building section:

// build segment list
        List<TimeLogSegmentDto> segments = minutesPerProject.entrySet().stream()
                .map(e -> {
                    Long projId = e.getKey();
                    Long mins = e.getValue();
                    BigDecimal hrs = BigDecimal.valueOf(mins).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_EVEN);

                    String projectName = null;
                    String projectShortCode = null;
                    String color = null;

                    try {
                        if (projId != -1L && projectRepository.existsById(projId)) {
                            var p = projectRepository.findById(projId).orElse(null);
                            if (p != null) {
                                projectName = p.getName();
                                projectShortCode = p.getShortCode(); // या जो भी फ़ील्ड नाम हो
                                // color = p.getColorCode(); // अगर color फ़ील्ड है
                            }
                        }
                    } catch (Exception ignored) {}

                    return TimeLogSegmentDto.builder()
                            .projectId(projId == -1L ? null : projId)
                            .projectName(projectName)
                            .projectShortCode(projectShortCode) // ✅ अब सेगमेंट में भी
                            .minutes(mins)
                            .hours(hrs)
                            .color(color)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalHours = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_EVEN);

        TimeLogDaySummaryDto summary = TimeLogDaySummaryDto.builder()
                .date(date)
                .totalMinutes(totalMinutes)
                .totalHours(totalHours)
                .segments(segments)
                .build();

        return TimeLogDayResponse.builder()
                .timeLogs(timeLogDtos)
                .summary(summary)
                .build();
    }

    @Override
    public List<TimeLogDaySummaryDto> getWeekSummaryForEmployee(String employeeId, LocalDate weekStartDate) {
        if (employeeId == null || weekStartDate == null) throw new IllegalArgumentException("employeeId and weekStartDate required");

        // authorize
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!hasAdminRole() && !currentUser.equals(employeeId)) {
            throw new AccessDeniedException("You can only view your own timelogs");
        }

        List<TimeLogDaySummaryDto> week = new ArrayList<>();
        LocalDate cur = weekStartDate;
        for (int i = 0; i < 7; i++) {
            // For each day compute summary using the same logic (could call previous method)
            TimeLogDayResponse dayResp = getTimeLogsForEmployeeOnDate(employeeId, cur);
            week.add(dayResp.getSummary());
            cur = cur.plusDays(1);
        }
        return week;
    }

    @Override
    public List<TimeLogDto> listByTask(Long taskId, String actor) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID is required");
        }

        // Admin can access all
        if (!hasAdminRole()) {
            // Non-admin: ensure actor is assigned to this task
            boolean assigned = false;
            try {
                assigned = taskRepository.isEmployeeAssignedToTask(actor, taskId);
            } catch (Exception e) {
                log.warn("Failed to check task assignment for {} on task {}: {}", actor, taskId, e.getMessage());
            }
            if (!assigned) {
                throw new AccessDeniedException("You are not assigned to task " + taskId);
            }
        }

        // Fetch timelogs
        List<TimeLog> timeLogs = repo.findByTaskId(taskId);
        return toDtoListWithEmployeeAndProjectDetails(timeLogs);
    }

    @Override
    @Transactional
    public WeeklyTimeLogCreateResponse createWeekly(String actor, WeeklyTimeLogCreateRequest req) {
        if (req == null || req.getDays() == null || req.getDays().isEmpty()) {
            throw new IllegalArgumentException("At least one day entry is required");
        }

        // Employee to fill = request ka employeeId (admin case) ya current user
        String employeeId = (req.getEmployeeId() == null || req.getEmployeeId().isBlank())
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : req.getEmployeeId();

//        // Non-admin only apne liye hi bhar sakta hai
//        if (!hasAdminRole() && !actor.equals(actor)) {
//            throw new AccessDeniedException("You can only fill your own weekly timesheet");
//        }

        if (req.getTaskId() == null) {
            throw new IllegalArgumentException("Task ID is required for weekly timelog");
        }

        Task task = taskRepository.findById(req.getTaskId()).orElse(null);

        // Employee assignment validate (project + task)
        validateEmployeeAssignment(employeeId, task.getProjectId(), req.getTaskId());

        List<TimeLogDto> created = new ArrayList<>();
        List<LocalDate> alreadyFilled = new ArrayList<>();
        List<LocalDate> invalidDates = new ArrayList<>();

        // Same start time for all entries = jab user ne weekly submit kiya
        LocalTime startTime = LocalTime.now();

        for (WeeklyTimeLogDayRequest dayReq : req.getDays()) {
            if (dayReq == null || dayReq.getDate() == null) {
                continue;
            }

            LocalDate date = dayReq.getDate();

            // hours missing / <= 0 -> skip + invalid list
            if (dayReq.getHours() == null || dayReq.getHours().compareTo(BigDecimal.ZERO) <= 0) {
                invalidDates.add(date);
                continue;
            }

            // Check existing timelog for this employee + task + date
            boolean exists = repo.existsByEmployeeIdAndTaskIdAndStartDate(employeeId, req.getTaskId(), date);
            if (exists) {
                alreadyFilled.add(date);
                continue;
            }

            // Hours -> minutes
            long minutes = dayReq.getHours()
                    .multiply(BigDecimal.valueOf(60))
                    .setScale(0, RoundingMode.HALF_EVEN)
                    .longValue();

            // End time = startTime + working duration
            LocalTime endTime = startTime.plusMinutes(minutes);

            TimeLog t = TimeLog.builder()
                    .employeeId(actor)
                    .projectId(task.getProjectId())
                    .taskId(req.getTaskId())
                    .startDate(date)
                    .startTime(startTime)
                    .endDate(date)
                    .endTime(endTime)
                    .memo(dayReq.getMemo())
                    .durationMinutes(minutes) // direct set, fir bhi safe hai
                    .build();

            t.setCreatedBy(actor);

            // Extra safety: recompute from start/end
            computeDuration(t);

            TimeLog saved = repo.save(t);

            // Notification + activity
            sendTimeLogNotification(actor, employeeId, t.getProjectId(), saved.getDurationMinutes(), "CREATED");
            activityService.record(t.getProjectId(), actor, "TIMELOG_CREATED", String.valueOf(saved.getId()));

            created.add(toDtoWithEmployeeAndProjectDetails(saved));
        }

        return WeeklyTimeLogCreateResponse.builder()
                .employeeId(employeeId)
                .projectId(req.getProjectId())
                .taskId(req.getTaskId())
                .createdLogs(created)
                .alreadyFilledDates(alreadyFilled)
                .skippedInvalidDates(invalidDates)
                .build();
    }
    //Adding new one

    @Transactional(readOnly = true)
    @Override
    public List<EmployeeTimesheetSummaryDto> getAllEmployeesTimesheetSummary() {
        // सिर्फ ADMIN या MANAGER ही access कर सकते हैं
        if (!hasAdminRole()) {
            throw new AccessDeniedException("Only admin or manager can view all employees timesheets");
        }

        // सभी unique employee IDs जिनके time logs हैं
        Set<String> employeeIds = repo.findAllDistinctEmployeeIds();

        if (employeeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<EmployeeTimesheetSummaryDto> result = new ArrayList<>();

        // Batch में employee details fetch करने के लिए (performance के लिए)
        Map<String, EmployeeMetaDto> employeeMetaMap = fetchEmployeeDetailsMap(employeeIds);

        for (String employeeId : employeeIds) {
            // इस employee के सभी time logs
            List<TimeLog> employeeTimeLogs = repo.findByEmployeeId(employeeId);

            if (!employeeTimeLogs.isEmpty()) {
                // Time logs को DTO में convert
                List<TimeLogDto> timeLogDtos = toDtoListWithEmployeeAndProjectDetails(employeeTimeLogs);

                // Total hours calculate
                Long totalMinutes = employeeTimeLogs.stream()
                        .map(TimeLog::getDurationMinutes)
                        .filter(Objects::nonNull)
                        .mapToLong(Long::longValue)
                        .sum();

                BigDecimal totalHours = BigDecimal.valueOf(totalMinutes)
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_EVEN);

                // Employee details
                EmployeeMetaDto meta = employeeMetaMap.get(employeeId);
                String employeeName = (meta != null && meta.getName() != null) ? meta.getName() : employeeId;
                String employeeEmail = (meta != null && meta.getEmployeeId() != null) ?
                        meta.getEmployeeId() + "@company.com" : "N/A";
                String designation = (meta != null) ? meta.getDesignation() : null;
                String department = (meta != null) ? meta.getDepartment() : null;

                // DTO create करें
                EmployeeTimesheetSummaryDto summary = EmployeeTimesheetSummaryDto.builder()
                        .employeeId(employeeId)
                        .employeeName(employeeName)
                        .designation(designation)
                        .employeeEmail(employeeEmail)
                        .totalMinutes(totalMinutes)
                        .totalHours(totalHours)
                        .timeLogs(timeLogDtos)  // सारे time logs array में
                        .build();

                result.add(summary);
            }
        }

        // Optional: employee name से sort करें
        result.sort(Comparator.comparing(EmployeeTimesheetSummaryDto::getEmployeeName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return result;
    }


    @Transactional(readOnly = true)
    @Override
    public EmployeeTimesheetSummaryDto getMyTimesheetSummary(String employeeId) {

        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        // 🔒 extra safety (employee apna hi dekh sake)
        String currentUser = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        if (!hasAdminRole() && !currentUser.equals(employeeId)) {
            throw new AccessDeniedException("You can only view your own timesheet");
        }

        List<TimeLog> employeeTimeLogs = repo.findByEmployeeId(employeeId);

        if (employeeTimeLogs.isEmpty()) {
            return EmployeeTimesheetSummaryDto.builder()
                    .employeeId(employeeId)
                    .totalMinutes(0L)
                    .totalHours(BigDecimal.ZERO.setScale(2))
                    .timeLogs(Collections.emptyList())
                    .build();
        }

        // Convert to DTO
        List<TimeLogDto> timeLogDtos =
                toDtoListWithEmployeeAndProjectDetails(employeeTimeLogs);

        // Total minutes
        Long totalMinutes = employeeTimeLogs.stream()
                .map(TimeLog::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        BigDecimal totalHours = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_EVEN);

        // Employee meta
        EmployeeMetaDto meta = null;
        try {
            meta = employeeClient.getMeta(employeeId);
        } catch (Exception ignored) {}

        return EmployeeTimesheetSummaryDto.builder()
                .employeeId(employeeId)
                .employeeName(meta != null ? meta.getName() : employeeId)
                .designation(meta != null ? meta.getDesignation() : null)
                .employeeEmail(meta != null
                        ? meta.getEmployeeId() + "@company.com"
                        : "N/A")
                .totalMinutes(totalMinutes)
                .totalHours(totalHours)
                .timeLogs(timeLogDtos)
                .build();
    }




    // NEW METHOD: Validate user has access to project
    private void validateUserProjectAccess(String userId, Long projectId) {
        if (projectId == null) {
            throw new AccessDeniedException("Project ID is required");
        }

        // Check if user is admin
        if (hasAdminRole()) {
            return; // Admin has access to all projects
        }

        // Check if user is assigned to the project
        boolean isAssignedToProject = projectRepository.isEmployeeAssignedToProject(projectId, userId);
        if (!isAssignedToProject) {
            throw new AccessDeniedException("You are not assigned to project " + projectId);
        }
    }


    @Override
    public Long sumDurationMinutesByProject(Long projectId) {
        Long s = repo.sumDurationMinutesByProjectId(projectId);
        return s == null ? 0L : s;
    }

    // UPDATED METHOD: Convert single TimeLog to DTO with employee details AND project shortcode
    private TimeLogDto toDtoWithEmployeeAndProjectDetails(TimeLog timeLog) {
        TimeLogDto dto = TimeLogMapper.toDto(timeLog);

        if(dto.getTaskId() == null) {
            try {
                Task task = taskRepository.findById(timeLog.getTaskId()).orElse(null);
                dto.setTaskName(task.getTitle());
            }
            catch (Exception e) {
                log.warn("Failed to fetch project shortcode for projectId: {}", timeLog.getTaskId(), e);
            }
        }

        // Fetch employee details
        try {
            EmployeeMetaDto employeeDetail = employeeClient.getMeta(timeLog.getEmployeeId());
            if (employeeDetail != null) {
                dto.setEmployees(List.of(employeeDetail));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch employee details for ID: {}", timeLog.getEmployeeId(), e);
            // Continue without employee details
        }

        // NEW: Fetch project shortcode
        if (timeLog.getProjectId() != null) {
            try {
                String shortCode = projectRepository.findShortCodeById(timeLog.getProjectId());
                String projectName = projectRepository.findById(timeLog.getProjectId()).get().getName();
                dto.setProjectName(projectName);
                dto.setProjectShortCode(shortCode);
            } catch (Exception e) {
                log.warn("Failed to fetch project shortcode for projectId: {}", timeLog.getProjectId(), e);
            }
        }

        return dto;
    }

    // UPDATED METHOD: Convert list of TimeLogs to DTOs with employee details AND project shortcode
//    private List<TimeLogDto> toDtoListWithEmployeeAndProjectDetails(List<TimeLog> timeLogs) {
//        if (timeLogs == null || timeLogs.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        // Extract unique employee IDs
//        Set<String> employeeIds = timeLogs.stream()
//                .map(TimeLog::getEmployeeId)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toSet());
//
//        // Extract unique project IDs for batch fetching shortcodes
//        Set<Long> projectIds = timeLogs.stream()
//                .map(TimeLog::getProjectId)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toSet());
//
//        // Fetch employee details and create a map
//        Map<String, EmployeeMetaDto> employeeMap = fetchEmployeeDetailsMap(employeeIds);
//
//        // NEW: Fetch project shortcodes and create a map
//        Map<Long, String> projectShortCodeMap = fetchProjectShortCodesMap(projectIds);
//
//        // Convert to DTOs
//        return timeLogs.stream().map(timeLog -> {
//            TimeLogDto dto = TimeLogMapper.toDto(timeLog);
//
//            // Set employee details
//            EmployeeMetaDto employeeDetail = employeeMap.get(timeLog.getEmployeeId());
//            if (employeeDetail != null) {
//                dto.setEmployees(List.of(employeeDetail));
//            }
//
//            // NEW: Set project shortcode
//            String shortCode = projectShortCodeMap.get(timeLog.getProjectId());
//            if (shortCode != null) {
//                dto.setProjectShortCode(shortCode);
//            }
//
//            return dto;
//        }).collect(Collectors.toList());
//    }

    // UPDATED METHOD: Convert list of TimeLogs to DTOs with employee details AND project shortcode
    private List<TimeLogDto> toDtoListWithEmployeeAndProjectDetails(List<TimeLog> timeLogs) {
        if (timeLogs == null || timeLogs.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract unique IDs
        Set<String> employeeIds = timeLogs.stream()
                .map(TimeLog::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> projectIds = timeLogs.stream()
                .map(TimeLog::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> taskIds = timeLogs.stream()
                .map(TimeLog::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Fetch all data in batches
        Map<String, EmployeeMetaDto> employeeMap = fetchEmployeeDetailsMap(employeeIds);
        Map<Long, String> projectNameMap = fetchProjectNamesMap(projectIds);
        Map<Long, String> projectShortCodeMap = fetchProjectShortCodesMap(projectIds);
        Map<Long, String> taskNameMap = fetchTaskNamesMap(taskIds);

        // Convert to DTOs
        return timeLogs.stream().map(timeLog -> {
            TimeLogDto dto = TimeLogMapper.toDto(timeLog);

            // Set employee details
            EmployeeMetaDto employeeDetail = employeeMap.get(timeLog.getEmployeeId());
            if (employeeDetail != null) {
                dto.setEmployees(List.of(employeeDetail));
            }

            // Set project details
            if (timeLog.getProjectId() != null) {
                dto.setProjectName(projectNameMap.get(timeLog.getProjectId()));
                dto.setProjectShortCode(projectShortCodeMap.get(timeLog.getProjectId()));
            }

            // Set task name
            if (timeLog.getTaskId() != null) {
                dto.setTaskName(taskNameMap.get(timeLog.getTaskId()));
            }

            return dto;
        }).collect(Collectors.toList());
    }


    // NEW METHOD: Fetch project names and return as Map
    private Map<Long, String> fetchProjectNamesMap(Set<Long> projectIds) {
        Map<Long, String> projectMap = new HashMap<>();

        if (projectIds == null || projectIds.isEmpty()) {
            return projectMap;
        }

        // Fetch all projects at once
        try {
            List<com.erp.project_service.entity.Project> projects = projectRepository.findAllById(projectIds);
            for (var project : projects) {
                projectMap.put(project.getId(), project.getName());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch project names: {}", e.getMessage());
        }

        return projectMap;
    }

    // NEW METHOD: Fetch task names and return as Map
    private Map<Long, String> fetchTaskNamesMap(Set<Long> taskIds) {
        Map<Long, String> taskMap = new HashMap<>();

        if (taskIds == null || taskIds.isEmpty()) {
            return taskMap;
        }

        // Fetch all tasks at once
        try {
            List<Task> tasks = taskRepository.findAllById(taskIds);
            for (var task : tasks) {
                taskMap.put(task.getId(), task.getTitle());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch task names: {}", e.getMessage());
        }

        return taskMap;
    }

    // NEW METHOD: Fetch project shortcodes and return as Map
    private Map<Long, String> fetchProjectShortCodesMap(Set<Long> projectIds) {
        Map<Long, String> projectMap = new HashMap<>();

        if (projectIds == null || projectIds.isEmpty()) {
            return projectMap;
        }

        // Fetch each project shortcode individually
        for (Long projectId : projectIds) {
            try {
                String shortCode = projectRepository.findShortCodeById(projectId);
                if (shortCode != null) {
                    projectMap.put(projectId, shortCode);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch project shortcode for ID: {}", projectId, e);
            }
        }

        return projectMap;
    }

    // Existing method - unchanged
    private Map<String, EmployeeMetaDto> fetchEmployeeDetailsMap(Set<String> employeeIds) {
        Map<String, EmployeeMetaDto> employeeMap = new HashMap<>();

        if (employeeIds == null || employeeIds.isEmpty()) {
            return employeeMap;
        }

        // Fetch each employee detail individually
        for (String employeeId : employeeIds) {
            try {
                EmployeeMetaDto employeeDetail = employeeClient.getMeta(employeeId);
                if (employeeDetail != null) {
                    employeeMap.put(employeeId, employeeDetail);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch employee details for ID: {}", employeeId, e);
                // Continue with other employees
            }
        }

        return employeeMap;
    }

    private void validateEmployeeAssignment(String employeeId, Long projectId, Long taskId) {
        // If taskId is provided, check task assignment
        if (taskId != null) {
            boolean isAssignedToTask = taskRepository.isEmployeeAssignedToTask(employeeId, taskId);
            if (!isAssignedToTask) {
                throw new AccessDeniedException("Employee " + employeeId + " is not assigned to task " + taskId);
            }
        }

        // If projectId is provided, check project assignment
        if (projectId != null) {
            boolean isAssignedToProject = projectRepository.isEmployeeAssignedToProject(projectId, employeeId);
            if (!isAssignedToProject) {
                throw new AccessDeniedException("Employee " + employeeId + " is not assigned to project " + projectId);
            }
        }

        // If both are null, it's okay (general timelog without specific project/task)
    }

    private void computeDuration(TimeLog t) {
        if (t.getStartDate() != null && t.getEndDate() != null && t.getStartTime() != null && t.getEndTime() != null) {
            try {
                LocalDateTime startDateTime = LocalDateTime.of(t.getStartDate(), t.getStartTime());
                LocalDateTime endDateTime = LocalDateTime.of(t.getEndDate(), t.getEndTime());

                long minutes = Duration.between(startDateTime, endDateTime).toMinutes();
                t.setDurationMinutes(Math.max(0L, minutes));

            } catch (Exception ex) {
                log.error("Error computing duration for timelog: {}", t.getId(), ex);
                t.setDurationMinutes(0L);
            }
        } else {
            t.setDurationMinutes(0L);
        }
    }

    // Helper method to check if current user has admin role
    private boolean hasAdminRole() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }
}