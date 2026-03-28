package com.erp.employee_service.service.attendance;

import com.erp.employee_service.dto.attendance.AttendancePayload;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.attendance.Attendance;
import com.erp.employee_service.entity.attendance.AttendanceActivity;
import com.erp.employee_service.entity.holiday.Holiday;
import com.erp.employee_service.entity.leave.Leave;
import com.erp.employee_service.entity.leave.LeaveStatus;
import com.erp.employee_service.repository.AttendanceActivityRepository;
import com.erp.employee_service.repository.AttendanceRepository;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.repository.HolidayRepository;
import com.erp.employee_service.repository.LeaveRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceActivityService {

    private final AttendanceActivityRepository activityRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRepository leaveRepository;
    private final HolidayRepository holidayRepository;

    /**
     * Create an activity (IN or OUT) for an employee on a date.
     *
     * Rules enforced:
     * - If an Attendance row exists and markedBy != null (admin marked), disallow employee action.
     * - Only one IN per date allowed. Further IN attempts -> error.
     * - Only one OUT per date allowed. Further OUT attempts -> error.
     * - OUT requires prior IN (employee must have clocked in first).
     * - Otherwise, link to existing Attendance or create a new Attendance summary row and link to it.
     * - After saving activity, recompute attendance summary fields (clockInTime, clockInLocation, clockInWorkingFrom,
     *   clockOutTime, clockOutLocation, clockOutWorkingFrom, late, halfDay, isPresent) **while respecting leave/holiday**.
     */
    @Transactional
    public AttendanceActivity createActivity(String employeeId, LocalDate date, String type, LocalTime time, String location, String workingFrom) {
        // Normalize type
        String t = (type == null) ? "" : type.trim().toUpperCase();

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        // Find existing Attendance (if any)
        Optional<Attendance> existingAttendanceOpt = attendanceRepository.findByEmployeeAndDate(emp, date);

        // business rule: if an Attendance row exists and markedBy != null (admin marked), disallow employee action
        existingAttendanceOpt.ifPresent(a -> {
            if (a.getMarkedBy() != null) {
                throw new IllegalArgumentException("Attendance for date " + date + " has been marked by admin. Please contact admin or ask them to delete it before you can clock in/out.");
            }
        });
        // Fetch existing activities for this employee+date for checks
        List<AttendanceActivity> todaysActivities = activityRepository.findByEmployeeAndDateOrderByCreatedAtAsc(emp, date);

        // Enforce single IN / single OUT rules and OUT-after-IN rule
        boolean hasIn = todaysActivities.stream().anyMatch(a -> "IN".equalsIgnoreCase(a.getType()));
        boolean hasOut = todaysActivities.stream().anyMatch(a -> "OUT".equalsIgnoreCase(a.getType()));

        if ("IN".equals(t)) {
            if (hasIn) {
                throw new IllegalArgumentException("You have already clocked IN for date " + date + ". Only one clock-in is allowed per date.");
            }
        } else if ("OUT".equals(t)) {
            if (!hasIn) {
                throw new IllegalArgumentException("Cannot clock OUT before clocking IN for date " + date + ". Please clock in first.");
            }
            if (hasOut) {
                throw new IllegalArgumentException("You have already clocked OUT for date " + date + ". Only one clock-out is allowed per date.");
            }
        } else {
            throw new IllegalArgumentException("Invalid activity type. Use 'IN' or 'OUT'.");
        }

        // Use existing attendance or create a new summary attendance row
        Attendance attendance = existingAttendanceOpt.orElseGet(() -> {
            Attendance newAt = Attendance.builder()
                    .employee(emp)
                    .date(date)
                    .isPresent(Boolean.TRUE)
                    .overwritten(Boolean.FALSE)
                    .build();
            return attendanceRepository.save(newAt);
        });

        // create activity and link to attendance summary
        AttendanceActivity act = AttendanceActivity.builder()
                .employee(emp)
                .date(date)
                .type(t)
                .time(time)
                .location(location)
                .workingFrom(workingFrom)
                .attendance(attendance)
                .build();

        AttendanceActivity saved = activityRepository.save(act);

        // Recompute attendance summary based on all activities for that employee+date
        recomputeAndSaveAttendanceSummary(attendance, emp, date);

        return saved;
    }

    /**
     * Recompute summary attendance fields (clockInTime, clockInLocation, clockInWorkingFrom,
     * clockOutTime, clockOutLocation, clockOutWorkingFrom, isPresent, late, halfDay)
     * based on existing activities for the employee on that date.
     *
     * Rules (modifiable):
     * - clockInTime = earliest activity with type "IN"
     * - clockInLocation/clockInWorkingFrom from that earliest IN activity
     * - clockOutTime = latest activity with type "OUT"
     * - clockOutLocation/clockOutWorkingFrom from that latest OUT activity
     * - isPresent = true if at least one IN or OUT exists, BUT if there is an APPROVED leave or ACTIVE holiday and attendance.overwritten != true,
     *                keep isPresent = false (leave/holiday precedence)
     * - late = true if clockInTime != null and clockInTime > 09:30
     * - halfDay = true if both IN and OUT exist and duration between them < 4 hours
     */
    @Transactional
    protected void recomputeAndSaveAttendanceSummary(Attendance attendance, Employee emp, LocalDate date) {
        // fetch all activities for that employee+date
        List<AttendanceActivity> activities = activityRepository.findByEmployeeAndDateOrderByCreatedAtAsc(emp, date);

        // earliest IN activity (object) and its time/location/workingFrom
        Optional<AttendanceActivity> firstInAct = activities.stream()
                .filter(a -> "IN".equalsIgnoreCase(a.getType()))
                .min(Comparator.comparing(AttendanceActivity::getTime));

        // latest OUT activity (object)
        Optional<AttendanceActivity> lastOutAct = activities.stream()
                .filter(a -> "OUT".equalsIgnoreCase(a.getType()))
                .max(Comparator.comparing(AttendanceActivity::getTime));

        // Set times and locations from activities
        attendance.setClockInTime(firstInAct.map(AttendanceActivity::getTime).orElse(null));
        attendance.setClockInLocation(firstInAct.map(AttendanceActivity::getLocation).orElse(null));
        attendance.setClockInWorkingFrom(firstInAct.map(AttendanceActivity::getWorkingFrom).orElse(null));

        attendance.setClockOutTime(lastOutAct.map(AttendanceActivity::getTime).orElse(null));
        attendance.setClockOutLocation(lastOutAct.map(AttendanceActivity::getLocation).orElse(null));
        attendance.setClockOutWorkingFrom(lastOutAct.map(AttendanceActivity::getWorkingFrom).orElse(null));

        // default present if any IN or OUT exists
        boolean hasIn = firstInAct.isPresent();
        boolean hasOut = lastOutAct.isPresent();
        boolean isPresent = hasIn || hasOut;

        // Check leave/holiday precedence:
        boolean isHoliday = holidayRepository.findAll().stream()
                .anyMatch(h -> Boolean.TRUE.equals(h.getIsActive()) && date.equals(h.getDate()));

        boolean hasApprovedLeave = leaveRepository.findAll().stream().anyMatch(l ->
                l.getEmployee() != null &&
                        l.getEmployee().getEmployeeId().equals(emp.getEmployeeId()) &&
                        l.getStatus() == LeaveStatus.APPROVED &&
                        (
                                (l.getSingleDate() != null && l.getSingleDate().equals(date)) ||
                                        (l.getStartDate() != null && l.getEndDate() != null &&
                                                (!date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate())))
                        )
        );

        // If holiday or leave exists and attendance NOT overwritten -> mark not present
        if ((isHoliday || hasApprovedLeave) && (attendance.getOverwritten() == null || !attendance.getOverwritten())) {
            attendance.setIsPresent(Boolean.FALSE);
        } else {
            attendance.setIsPresent(isPresent);
        }

        // late: simple rule (after 09:30)
        if (attendance.getClockInTime() != null) {
            LocalTime threshold = LocalTime.of(9, 30);
            attendance.setLate(attendance.getClockInTime().isAfter(threshold));
        } else {
            attendance.setLate(Boolean.FALSE);
        }

        // halfDay: if both in and out present and duration < 4 hours
        if (attendance.getClockInTime() != null && attendance.getClockOutTime() != null) {
            Duration dur = Duration.between(attendance.getClockInTime(), attendance.getClockOutTime());
            attendance.setHalfDay(dur.toMinutes() < (4 * 60));
        } else {
            attendance.setHalfDay(Boolean.FALSE);
        }

        // Keep overwritten/markedBy as-is (admin changes should remain)
        attendanceRepository.save(attendance);
    }

    @Transactional(readOnly = true)
    public List<AttendanceActivity> getActivitiesForDate(String employeeId, LocalDate date) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        return activityRepository.findByEmployeeAndDateOrderByCreatedAtAsc(emp, date);
    }

    @Transactional(readOnly = true)
    public List<AttendanceActivity> getActivitiesBetween(String employeeId, LocalDate from, LocalDate to) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        return activityRepository.findByEmployeeAndDateBetweenOrderByDateAscCreatedAtAsc(emp, from, to);
    }

    // ---------------------------
    // DTO mapping and DTO APIs
    // ---------------------------

    @Data
    public static class AttendanceActivityDTO {
        private Long id;
        private String employeeId;
        private String employeeName;
        private LocalDate date;
        private String type;
        private LocalTime time;
        private String location;
        private String workingFrom;
        private Long attendanceId;
        private String createdAt; // optional ISO string; you can format as needed
    }

    private AttendanceActivityDTO toDto(AttendanceActivity a) {
        if (a == null) return null;
        AttendanceActivityDTO dto = new AttendanceActivityDTO();
        dto.setId(a.getId());
        if (a.getEmployee() != null) {
            dto.setEmployeeId(a.getEmployee().getEmployeeId());
            dto.setEmployeeName(a.getEmployee().getName());
        }
        dto.setDate(a.getDate());
        dto.setType(a.getType());
        dto.setTime(a.getTime());
        dto.setLocation(a.getLocation());
        dto.setWorkingFrom(a.getWorkingFrom());
        if (a.getAttendance() != null) dto.setAttendanceId(a.getAttendance().getId());
        if (a.getCreatedAt() != null) dto.setCreatedAt(a.getCreatedAt().toString());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<AttendanceActivityDTO> getActivitiesForDateDto(String employeeId, LocalDate date) {
        List<AttendanceActivity> acts = getActivitiesForDate(employeeId, date);
        return acts.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceActivityDTO> getActivitiesBetweenDto(String employeeId, LocalDate from, LocalDate to) {
        List<AttendanceActivity> acts = getActivitiesBetween(employeeId, from, to);
        return acts.stream().map(this::toDto).collect(Collectors.toList());
    }
}
