package com.erp.employee_service.controller.attendance;

import com.erp.employee_service.dto.attendance.*;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.attendance.Attendance;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.AttendanceRepository;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.service.attendance.AttendanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee/attendance")
@Slf4j
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;

    public AttendanceController(AttendanceService attendanceService,
                                EmployeeRepository employeeRepository,
                                AttendanceRepository attendanceRepository) {
        this.attendanceService = attendanceService;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
    }

    // ADMIN-only marking endpoints
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mark")
    public ResponseEntity<Map<String, Map<LocalDate, String>>> markAttendance(@RequestBody BulkAttendanceRequest request) {
        try {
            Map<String, Map<LocalDate, String>> result = attendanceService.markAttendanceForEmployees(
                    request.getEmployeeIds(),
                    request.getDates(),
                    request.getPayload(),
                    request.isOverwrite(),
                    request.getMarkedBy()
            );
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mark/by-employees")
    public ResponseEntity<Map<String, Map<LocalDate, String>>> markAttendanceByEmployees(@RequestBody BulkByEmployeesRequest request) {
        try {
            Map<String, Map<LocalDate, String>> result = attendanceService.markAttendanceForEmployees(
                    request.getEmployeeIds(),
                    request.getDates(),
                    request.getPayload(),
                    request.isOverwrite(),
                    request.getMarkedBy()
            );
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mark/month")
    public ResponseEntity<Map<String, Map<LocalDate, String>>> markAttendanceForMonth(@RequestBody MonthAttendanceRequest request) {
        try {
            Map<String, Map<LocalDate, String>> result = attendanceService.markAttendanceForMonth(
                    request.getYear(),
                    request.getMonth(),
                    request.getEmployeeIds(),
                    request.getPayload(),
                    request.isOverwrite(),
                    request.getMarkedBy()
            );
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /*
      It give all attendance
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/GetAllAttendance")
    public ResponseEntity<List<AttendanceMarkResult>> getAllAttendance() {
        return ResponseEntity.ok().body(attendanceService.getAllSavedAttendance());
    }

    @GetMapping("/{attendanceId}")
    public ResponseEntity<AttendanceDTO> getAttendance(@PathVariable Long attendanceId) {
        return ResponseEntity.ok().body(attendanceService.getAttendanceBYId(attendanceId));
    }

    /**
     * ADMIN: get saved attendance rows between dates for list of employees
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/between")
    public ResponseEntity<List<AttendanceMarkResult>> getBetween(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam("employeeIds") List<String> employeeIds
    ) {
        try {
            List<AttendanceMarkResult> result = attendanceService.getAttendanceBetween(from, to, employeeIds);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * EMPLOYEE: get their saved attendance rows (all)
     */
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<List<AttendanceMarkResult>> getMyAllAttendance(Authentication authentication) {
        try {
            String empId = authentication.getName(); // JWT principal = employeeId
            Employee emp = employeeRepository.findById(empId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + empId));

            List<AttendanceMarkResult> result = attendanceService.getAllSavedAttendanceForEmployee(emp);
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get attendance summary for an employee
     */
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAttendanceSummary(
            @RequestParam(value = "employeeId", required = false) String employeeId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        try {
            // If employeeId not provided, use the authenticated user's ID
            if (employeeId == null) {
                employeeId = authentication.getName();
            }

            Map<String, Object> summary = attendanceService.getAttendanceSummary(employeeId, from, to);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Check if attendance exists for an employee on a specific date
     */
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkAttendanceExists(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        boolean exists = attendanceRepository.existsByEmployeeIdAndDate(employeeId, date);
        return ResponseEntity.ok(exists);
    }

    /**
     * Delete attendance for specific employee and date
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteAttendance(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            attendanceService.deleteAttendance(employeeId, date);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get attendance calendar for employee - returns all dates in range with attendance status
     */
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @GetMapping("/calendar")
    public ResponseEntity<Map<LocalDate, AttendanceMarkResult>> getAttendanceCalendar(
            @RequestParam(value = "employeeId", required = false) String employeeId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {

        try {
            // If employeeId not provided, use the authenticated user's ID
            if (employeeId == null) {
                employeeId = authentication.getName();
            }

            Map<LocalDate, AttendanceMarkResult> calendar = attendanceService.getAttendanceCalendar(employeeId, from, to);
            return ResponseEntity.ok(calendar);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{employeeId}/all-saved")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllSavedAttendanceForEmployee(@PathVariable String employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElse(null);

        if (emp == null) {
            return ResponseEntity.badRequest().body("Employee not found: " + employeeId);
        }

        List<AttendanceMarkResult> results = attendanceService.getAllSavedAttendanceForEmployee(emp);
        return ResponseEntity.ok(results);
    }

    /**
     * ADMIN: Get employees who worked from home on a specific date.
     * Example: GET /employee/attendance/wfh?date=2025-11-10
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/wfh")
    public ResponseEntity<List<com.erp.employee_service.dto.attendance.AttendanceDTO>> getWorkFromHomeOnDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<com.erp.employee_service.dto.attendance.AttendanceDTO> result = attendanceService.getWorkFromHomeOnDate(date);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to fetch WFH attendances for date {}: {}", date, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * ADMIN: Get employees who worked from home between dates (inclusive).
     * Example: GET /employee/attendance/wfh/between?from=2025-11-01&to=2025-11-10
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/wfh/between")
    public ResponseEntity<List<com.erp.employee_service.dto.attendance.AttendanceDTO>> getWorkFromHomeBetween(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            List<com.erp.employee_service.dto.attendance.AttendanceDTO> result = attendanceService.getWorkFromHomeBetween(from, to);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to fetch WFH attendances between {} and {}: {}", from, to, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/edit")
    public ResponseEntity<?> editAttendanceForEmployeeDate(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody AttendancePayload payload,
            @RequestParam(value = "overwrite", defaultValue = "true") boolean overwrite,
            @RequestParam(value = "markedBy", required = false) String markedById) {
        try {
            // Load employee and existing attendance (or create)
            Employee emp = employeeRepository.findById(employeeId).orElse(null);
            if (emp == null) return ResponseEntity.badRequest().body("Employee not found: " + employeeId);

            Attendance at = attendanceRepository.findByEmployeeAndDate(emp, date)
                    .orElseGet(() -> Attendance.builder().employee(emp).date(date).build());

            // Fill fields from payload
            at.setClockInTime(payload.getClockInTime());
            at.setClockInLocation(payload.getClockInLocation());
            at.setClockInWorkingFrom(payload.getClockInWorkingFrom());
            at.setClockOutTime(payload.getClockOutTime());
            at.setClockOutLocation(payload.getClockOutLocation());
            at.setClockOutWorkingFrom(payload.getClockOutWorkingFrom());
            at.setLate(payload.getLate());
            at.setHalfDay(payload.getHalfDay());

            at.setOverwritten(overwrite);
            at.setIsPresent(Boolean.TRUE);

            if (markedById != null) {
                Employee markedBy = employeeRepository.findById(markedById).orElse(null);
                at.setMarkedBy(markedBy);
            }

            attendanceRepository.save(at);

            return ResponseEntity.ok(Map.of("message", "Attendance updated"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update attendance"));
        }
    }


}