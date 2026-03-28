package com.erp.employee_service.controller.attendance;

import com.erp.employee_service.dto.attendance.AttendancePayload;
import com.erp.employee_service.entity.attendance.AttendanceActivity;
import com.erp.employee_service.service.attendance.AttendanceActivityService;
import com.erp.employee_service.service.attendance.AttendanceActivityService.AttendanceActivityDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee/attendance/clock")
@RequiredArgsConstructor
public class EmployeeClockController {

    private final AttendanceActivityService activityService;

    /**
     * Employee clock in endpoint.
     * Body: { "time":"HH:mm:ss" (optional - server can use now), "location": "...", "workingFrom": "Home" }
     * Authenticated user principal name must be employeeId (same as your other controllers).
     */
    @PostMapping("/in")
    public ResponseEntity<?> clockIn(@RequestBody AttendancePayload payload,
                                     @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     Authentication authentication) {
        try {
            String employeeId = authentication.getName();
            LocalDate targetDate = (date != null) ? date : LocalDate.now();
            LocalTime time = (payload.getClockInTime() != null) ? payload.getClockInTime() : LocalTime.now();

            AttendanceActivity act = activityService.createActivity(employeeId, targetDate, "IN", time,
                    payload.getClockInLocation(), payload.getClockInWorkingFrom());

            return ResponseEntity.ok(Map.of("message", "Clock-in recorded", "activityId", act.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to record clock-in"));
        }
    }

    /**
     * Employee clock out endpoint.
     */
    @PostMapping("/out")
    public ResponseEntity<?> clockOut(@RequestBody AttendancePayload payload,
                                      @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      Authentication authentication) {
        try {
            String employeeId = authentication.getName();
            LocalDate targetDate = (date != null) ? date : LocalDate.now();
            LocalTime time = (payload.getClockOutTime() != null) ? payload.getClockOutTime() : LocalTime.now();

            AttendanceActivity act = activityService.createActivity(employeeId, targetDate, "OUT", time,
                    payload.getClockOutLocation(), payload.getClockOutWorkingFrom());

            return ResponseEntity.ok(Map.of("message", "Clock-out recorded", "activityId", act.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to record clock-out"));
        }
    }

    /**
     * Get activities for a specific date for the authenticated employee.
     * Returns DTOs to avoid returning lazy JPA entities directly.
     */
    @GetMapping("/activities")
    public ResponseEntity<?> getActivities(
            @RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        try {
            String employeeId = authentication.getName();
            List<AttendanceActivityDTO> activities = activityService.getActivitiesForDateDto(employeeId, date);
            return ResponseEntity.ok(activities);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch activities"));
        }
    }
}
