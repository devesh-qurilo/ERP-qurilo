package com.erp.employee_service.service.attendance;

import com.erp.employee_service.dto.attendance.AttendanceDTO;
import com.erp.employee_service.dto.attendance.AttendanceMarkResult;
import com.erp.employee_service.dto.attendance.AttendancePayload;
import com.erp.employee_service.dto.notification.SendNotificationDto;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.attendance.Attendance;
import com.erp.employee_service.entity.holiday.Holiday;
import com.erp.employee_service.entity.leave.Leave;
import com.erp.employee_service.entity.leave.LeaveStatus;
import com.erp.employee_service.repository.*;
import com.erp.employee_service.service.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRepository leaveRepository;
    private final HolidayRepository holidayRepository;
    private final NotificationService notificationService;
    private final AttendanceActivityRepository attendanceActivityRepository; // 👈 inject this


    public AttendanceService(AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository,
                             LeaveRepository leaveRepository,
                             HolidayRepository holidayRepository,
                             NotificationService notificationService,
                             AttendanceActivityRepository attendanceActivityRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRepository = leaveRepository;
        this.holidayRepository = holidayRepository;
        this.notificationService = notificationService;
        this.attendanceActivityRepository = attendanceActivityRepository;
    }

    @Transactional
    public Map<String, Map<LocalDate, String>> markAttendanceForEmployees(
            List<String> employeeIds,
            List<LocalDate> dates,
            AttendancePayload payload,
            boolean overwrite,
            String markedByAdminId) {

        // Validate input
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new IllegalArgumentException("Employee IDs cannot be null or empty");
        }

        if (dates == null || dates.isEmpty()) {
            throw new IllegalArgumentException("Dates cannot be null or empty");
        }

        List<Employee> employees = employeeRepository.findAllById(employeeIds);
        if (employees.size() != employeeIds.size()) {
            throw new IllegalArgumentException("Some employees not found");
        }

        Employee markedBy = null;
        if (markedByAdminId != null) {
            markedBy = employeeRepository.findById(markedByAdminId)
                    .orElseThrow(() -> new IllegalArgumentException("Marked by admin not found: " + markedByAdminId));
        }

        Map<String, Map<LocalDate, String>> result = new LinkedHashMap<>();

        // Preload holidays and leaves for efficiency
        List<Holiday> holidays = holidayRepository.findAll();
        List<Leave> allLeaves = leaveRepository.findAll();

        // Process each employee
        for (Employee emp : employees) {
            Map<LocalDate, String> perDateResult = new LinkedHashMap<>();

            for (LocalDate date : dates) {
                // Check if it's a holiday
                boolean isHoliday = holidays.stream()
                        .anyMatch(h -> Boolean.TRUE.equals(h.getIsActive()) && date.equals(h.getDate()));

                // Check if employee has approved leave for this date
                boolean hasLeave = allLeaves.stream().anyMatch(l ->
                        l.getEmployee() != null &&
                                l.getEmployee().getEmployeeId().equals(emp.getEmployeeId()) &&
                                l.getStatus() == LeaveStatus.APPROVED &&
                                (
                                        (l.getSingleDate() != null && l.getSingleDate().equals(date)) ||
                                                (l.getStartDate() != null && l.getEndDate() != null &&
                                                        (!date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate())))
                                )
                );

                // Create or update attendance for ALL dates, regardless of holiday/leave status
                Optional<Attendance> opt = attendanceRepository.findByEmployeeAndDate(emp, date);
                Attendance at;
                if (opt.isPresent()) {
                    at = opt.get();
                    log.info("Updating existing attendance for employee {} on date {}", emp.getEmployeeId(), date);
                } else {
                    at = Attendance.builder()
                            .employee(emp)
                            .date(date)
                            .build();
                    log.info("Creating new attendance for employee {} on date {}", emp.getEmployeeId(), date);
                }

                // Determine status and set appropriate values
                String status;
                if (isHoliday && !overwrite) {
                    status = "HOLIDAY";
                    at.setIsPresent(Boolean.FALSE);
                    at.setOverwritten(false);
                    // Don't set clock in/out times for holidays unless overwritten
                    if (!overwrite) {
                        at.setClockInTime(null);
                        at.setClockInLocation(null);
                        at.setClockInWorkingFrom(null);
                        at.setClockOutTime(null);
                        at.setClockOutLocation(null);
                        at.setClockOutWorkingFrom(null);
                        at.setLate(false);
                        at.setHalfDay(false);
                    }
                } else if (hasLeave && !overwrite) {
                    status = "LEAVE";
                    at.setIsPresent(Boolean.FALSE);
                    at.setOverwritten(false);
                    // Don't set clock in/out times for leaves unless overwritten
                    if (!overwrite) {
                        at.setClockInTime(null);
                        at.setClockInLocation(null);
                        at.setClockInWorkingFrom(null);
                        at.setClockOutTime(null);
                        at.setClockOutLocation(null);
                        at.setClockOutWorkingFrom(null);
                        at.setLate(false);
                        at.setHalfDay(false);
                    }
                } else {
                    status = "PRESENT";
                    at.setIsPresent(Boolean.TRUE);
                    at.setOverwritten(overwrite);

                    // Set attendance details for present days
                    at.setClockInTime(payload.getClockInTime());
                    at.setClockInLocation(payload.getClockInLocation());
                    at.setClockInWorkingFrom(payload.getClockInWorkingFrom());
                    at.setClockOutTime(payload.getClockOutTime());
                    at.setClockOutLocation(payload.getClockOutLocation());
                    at.setClockOutWorkingFrom(payload.getClockOutWorkingFrom());
                    at.setLate(payload.getLate());
                    at.setHalfDay(payload.getHalfDay());
                }

                // If overwrite is true, set as present regardless of holiday/leave
                if (overwrite) {
                    status = "PRESENT";
                    at.setIsPresent(Boolean.TRUE);
                    at.setOverwritten(true);

                    // Set attendance details for overwritten days
                    at.setClockInTime(payload.getClockInTime());
                    at.setClockInLocation(payload.getClockInLocation());
                    at.setClockInWorkingFrom(payload.getClockInWorkingFrom());
                    at.setClockOutTime(payload.getClockOutTime());
                    at.setClockOutLocation(payload.getClockOutLocation());
                    at.setClockOutWorkingFrom(payload.getClockOutWorkingFrom());
                    at.setLate(payload.getLate());
                    at.setHalfDay(payload.getHalfDay());
                }

                if (markedBy != null) {
                    at.setMarkedBy(markedBy);
                }

                attendanceRepository.save(at);
                perDateResult.put(date, status);
            }

            result.put(emp.getEmployeeId(), perDateResult);

            // Send notification to employee about attendance marking
            sendAttendanceNotification(emp, markedBy, dates, overwrite);
        }

        return result;
    }

    @Transactional
    public Map<String, Map<LocalDate, String>> markAttendanceForMonth(
            int year,
            int month,
            List<String> employeeIds,
            AttendancePayload payload,
            boolean overwrite,
            String markedBy
    ) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<LocalDate> dates = new ArrayList<>();
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            dates.add(cur);
            cur = cur.plusDays(1);
        }

        return markAttendanceForEmployees(employeeIds, dates, payload, overwrite, markedBy);
    }

    /**
     * Return list of AttendanceMarkResult for saved attendance rows in DB between dates for the given employees.
     * This returns only stored attendance records (one row per saved attendance) — as requested.
     * Each result includes computed holiday/leave flags and isPresent according to precedence rules.
     */
    @Transactional(readOnly = true)
    public List<AttendanceMarkResult> getAttendanceBetween(LocalDate from, LocalDate to, List<String> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new IllegalArgumentException("Employee IDs cannot be null or empty");
        }

        // Fetch saved attendances for these employees in the range
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdsAndDateBetween(employeeIds, from, to);

        // Preload leaves & holidays to compute flags
        List<Holiday> holidays = holidayRepository.findAll();
        List<Leave> leaves = leaveRepository.findAll();

        List<AttendanceMarkResult> results = new ArrayList<>();

        // For each saved attendance row, create a DTO and compute holiday/leave/isPresent flags
        for (Attendance a : attendances) {
            AttendanceMarkResult r = convertToAttendanceMarkResult(a, holidays, leaves);
            results.add(r);
        }

        return results;
    }

    /**
     * Employee /me: return saved attendance rows for this employee (all time or you can limit)
     */
    @Transactional(readOnly = true)
    public List<AttendanceMarkResult> getAllSavedAttendanceForEmployee(Employee employee) {
        List<Attendance> attendances = attendanceRepository.findAllByEmployee(employee);

        // Preload holidays and leaves
        List<Holiday> holidays = holidayRepository.findAll();
        List<Leave> leaves = leaveRepository.findAll();

        List<AttendanceMarkResult> results = new ArrayList<>();
        for (Attendance a : attendances) {
            AttendanceMarkResult r = convertToAttendanceMarkResult(a, holidays, leaves);
            results.add(r);
        }

        return results;
    }

    /**
     * Helper method to convert Attendance entity to AttendanceMarkResult DTO
     */
    private AttendanceMarkResult convertToAttendanceMarkResult(Attendance a, List<Holiday> holidays, List<Leave> leaves) {
        AttendanceMarkResult r = new AttendanceMarkResult();
        r.setDate(a.getDate());
        r.setEmployeeId(a.getEmployee().getEmployeeId());
        r.setEmployeeName(a.getEmployee().getName());

        // Default status is PRESENT because record exists
        r.setStatus("PRESENT");
        r.setAttendanceId(a.getId());
        r.setOverwritten(a.getOverwritten());
        r.setLate(a.getLate());
        r.setHalfDay(a.getHalfDay());

        r.setClockInTime(a.getClockInTime());
        r.setClockInLocation(a.getClockInLocation());
        r.setClockInWorkingFrom(a.getClockInWorkingFrom());

        r.setClockOutTime(a.getClockOutTime());
        r.setClockOutLocation(a.getClockOutLocation());
        r.setClockOutWorkingFrom(a.getClockOutWorkingFrom());

        if (a.getMarkedBy() != null) {
            r.setMarkedById(a.getMarkedBy().getEmployeeId());
            r.setMarkedByName(a.getMarkedBy().getName());
        }

        // Compute holiday flag for that date
        boolean isHoliday = holidays.stream()
                .anyMatch(h -> Boolean.TRUE.equals(h.getIsActive()) && a.getDate().equals(h.getDate()));
        r.setHoliday(isHoliday);

        // Compute leave flag for that date for that employee
        boolean hasLeave = leaves.stream().anyMatch(l ->
                l.getEmployee() != null &&
                        l.getEmployee().getEmployeeId().equals(a.getEmployee().getEmployeeId()) &&
                        l.getStatus() == LeaveStatus.APPROVED &&
                        (
                                (l.getSingleDate() != null && l.getSingleDate().equals(a.getDate())) ||
                                        (l.getStartDate() != null && l.getEndDate() != null &&
                                                (!a.getDate().isBefore(l.getStartDate()) && !a.getDate().isAfter(l.getEndDate())))
                        )
        );
        r.setLeave(hasLeave);

        // Compute final isPresent according to precedence:
        // if holiday || leave AND not overwritten -> isPresent = false
        // else if attendance exists -> isPresent = true
        if ((isHoliday || hasLeave) && (a.getOverwritten() == null || !a.getOverwritten())) {
            r.setIsPresent(Boolean.FALSE);
            r.setStatus(isHoliday ? "HOLIDAY" : "LEAVE");
        } else {
            r.setIsPresent(Boolean.TRUE);
            r.setStatus("PRESENT");
        }

        return r;
    }

    /**
     * Get attendance summary for an employee between dates
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAttendanceSummary(String employeeId, LocalDate from, LocalDate to) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateRange(employeeId, from, to);

        // Preload holidays and leaves
        List<Holiday> holidays = holidayRepository.findAll();
        List<Leave> leaves = leaveRepository.findAll();

        int presentDays = 0;
        int absentDays = 0;
        int leaveDays = 0;
        int holidayDays = 0;
        int lateDays = 0;
        int halfDays = 0;

        for (Attendance a : attendances) {
            boolean isHoliday = holidays.stream()
                    .anyMatch(h -> Boolean.TRUE.equals(h.getIsActive()) && a.getDate().equals(h.getDate()));

            boolean hasLeave = leaves.stream().anyMatch(l ->
                    l.getEmployee() != null &&
                            l.getEmployee().getEmployeeId().equals(employeeId) &&
                            l.getStatus() == LeaveStatus.APPROVED &&
                            (
                                    (l.getSingleDate() != null && l.getSingleDate().equals(a.getDate())) ||
                                            (l.getStartDate() != null && l.getEndDate() != null &&
                                                    (!a.getDate().isBefore(l.getStartDate()) && !a.getDate().isAfter(l.getEndDate())))
                            )
            );

            if (isHoliday && (a.getOverwritten() == null || !a.getOverwritten())) {
                holidayDays++;
            } else if (hasLeave && (a.getOverwritten() == null || !a.getOverwritten())) {
                leaveDays++;
            } else if (Boolean.TRUE.equals(a.getIsPresent())) {
                presentDays++;
                if (Boolean.TRUE.equals(a.getLate())) lateDays++;
                if (Boolean.TRUE.equals(a.getHalfDay())) halfDays++;
            } else {
                absentDays++;
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("employeeId", employeeId);
        summary.put("employeeName", employee.getName());
        summary.put("fromDate", from);
        summary.put("toDate", to);
        summary.put("presentDays", presentDays);
        summary.put("absentDays", absentDays);
        summary.put("leaveDays", leaveDays);
        summary.put("holidayDays", holidayDays);
        summary.put("lateDays", lateDays);
        summary.put("halfDays", halfDays);
        summary.put("totalWorkingDays", presentDays + absentDays + leaveDays + holidayDays);

        return summary;
    }

//    /**
//     * Delete attendance for specific employee and date
//     */
//    @Transactional
//    public void deleteAttendance(String employeeId, LocalDate date) {
//        Employee employee = employeeRepository.findById(employeeId)
//                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
//
//        attendanceRepository.deleteByEmployeeAndDate(employee, date);
//        log.info("Deleted attendance for employee {} on date {}", employeeId, date);
//
//        // Send notification about attendance deletion
//        sendAttendanceDeletionNotification(employee, date);
//    }
    /**
     * Delete attendance for specific employee and date
     */
    @Transactional
    public void deleteAttendance(String employeeId, LocalDate date) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        // Pehle Attendance row nikaalo
        Optional<Attendance> opt = attendanceRepository.findByEmployeeAndDate(employee, date);
        if (opt.isEmpty()) {
            log.info("No attendance found for employee {} on date {} to delete", employeeId, date);
            return;
        }

        Attendance attendance = opt.get();

        // 1) Pehle saari activities delete karo jo is attendance se linked hain
        attendanceActivityRepository.deleteByAttendance(attendance);

        // 2) Ab attendance delete karo
        attendanceRepository.delete(attendance);

        log.info("Deleted attendance and activities for employee {} on date {}", employeeId, date);

        // 3) Notification bhejna hai to (optional)
        sendAttendanceDeletionNotification(employee, date);
    }


    /**
     * Get attendance calendar for employee - returns all dates in range with attendance status
     */
    @Transactional(readOnly = true)
    public Map<LocalDate, AttendanceMarkResult> getAttendanceCalendar(String employeeId, LocalDate from, LocalDate to) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        // Get all saved attendances in the range
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateRange(employeeId, from, to);

        // Preload holidays and leaves
        List<Holiday> holidays = holidayRepository.findAll();
        List<Leave> leaves = leaveRepository.findAll();

        Map<LocalDate, AttendanceMarkResult> calendar = new LinkedHashMap<>();

        // Generate all dates in the range
        LocalDate current = from;
        while (!current.isAfter(to)) {
            final LocalDate date = current;

            // Check if we have an attendance record for this date
            Optional<Attendance> attendanceOpt = attendances.stream()
                    .filter(a -> a.getDate().equals(date))
                    .findFirst();

            if (attendanceOpt.isPresent()) {
                // Convert existing attendance to result
                AttendanceMarkResult result = convertToAttendanceMarkResult(attendanceOpt.get(), holidays, leaves);
                calendar.put(date, result);
            } else {
                // Create a default result for dates without attendance records
                AttendanceMarkResult result = new AttendanceMarkResult();
                result.setDate(date);
                result.setEmployeeId(employeeId);
                result.setEmployeeName(employee.getName());
                result.setAttendanceId(null);
                result.setOverwritten(false);
                result.setLate(false);
                result.setHalfDay(false);

                // Check if it's a holiday
                boolean isHoliday = holidays.stream()
                        .anyMatch(h -> Boolean.TRUE.equals(h.getIsActive()) && date.equals(h.getDate()));
                result.setHoliday(isHoliday);

                // Check if employee has approved leave for this date
                boolean hasLeave = leaves.stream().anyMatch(l ->
                        l.getEmployee() != null &&
                                l.getEmployee().getEmployeeId().equals(employeeId) &&
                                l.getStatus() == LeaveStatus.APPROVED &&
                                (
                                        (l.getSingleDate() != null && l.getSingleDate().equals(date)) ||
                                                (l.getStartDate() != null && l.getEndDate() != null &&
                                                        (!date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate())))
                                )
                );
                result.setLeave(hasLeave);

                // Set status based on holiday/leave
                if (isHoliday) {
                    result.setStatus("HOLIDAY");
                    result.setIsPresent(false);
                } else if (hasLeave) {
                    result.setStatus("LEAVE");
                    result.setIsPresent(false);
                } else {
                    result.setStatus("ABSENT");
                    result.setIsPresent(false);
                }

                calendar.put(date, result);
            }

            current = current.plusDays(1);
        }

        return calendar;
    }

    /**
     * Send notification to employee about attendance marking
     */
    private void sendAttendanceNotification(Employee employee, Employee markedBy, List<LocalDate> dates, boolean overwrite) {
        try {
            String markedByName = markedBy != null ? markedBy.getName() : "System";
            String title = "Attendance Marked";
            String message = String.format("Your attendance has been marked by %s for %d date(s). %s",
                    markedByName,
                    dates.size(),
                    overwrite ? "(Overwritten mode)" : "");

            // Create SendNotificationDto
            SendNotificationDto notificationDto = new SendNotificationDto();
            notificationDto.setReceiverEmployeeId(employee.getEmployeeId());
            notificationDto.setTitle(title);
            notificationDto.setMessage(message);
            notificationDto.setType("ATTENDANCE");

            notificationService.sendNotification(
                    markedBy != null ? markedBy.getEmployeeId() : null,
                    notificationDto
            );
        } catch (Exception e) {
            log.error("Failed to send attendance notification to employee {}: {}",
                    employee.getEmployeeId(), e.getMessage());
        }
    }

    /**
     * Send notification about attendance deletion
     */
    private void sendAttendanceDeletionNotification(Employee employee, LocalDate date) {
        try {
            String title = "Attendance Deleted";
            String message = String.format("Your attendance record for date %s has been deleted.", date);

            // Create SendNotificationDto
            SendNotificationDto notificationDto = new SendNotificationDto();
            notificationDto.setReceiverEmployeeId(employee.getEmployeeId());
            notificationDto.setTitle(title);
            notificationDto.setMessage(message);
            notificationDto.setType("ATTENDANCE_DELETION");

            notificationService.sendNotification(
                    null, // system generated
                    notificationDto
            );
        } catch (Exception e) {
            log.error("Failed to send attendance deletion notification to employee {}: {}",
                    employee.getEmployeeId(), e.getMessage());
        }
    }

    //New............


//    public List<AttendanceDTO> getAllSavedAttendance() {
//        return attendanceRepository.findAllWithEmployees().stream()
//                .map(attendance -> new AttendanceDTO(attendance))
//                .collect(Collectors.toList());
//    }

    @Transactional(readOnly = true)
    public List<AttendanceMarkResult> getAllSavedAttendance() {

        // 1. Fetch all saved attendance rows
        List<Attendance> attendances = attendanceRepository.findAllWithEmployees();

        // 2. Preload holidays & leaves (same as calendar)
        List<Holiday> holidays = holidayRepository.findAll();
        List<Leave> leaves = leaveRepository.findAll();

        // 3. Convert using SAME logic as calendar
        return attendances.stream()
                .map(att -> convertToAttendanceMarkResult(att, holidays, leaves))
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<com.erp.employee_service.dto.attendance.AttendanceDTO> getWorkFromHomeOnDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("date required");

        // keyword to match - "home" is generic and will match WorkFromHome, Home, WFH-containing strings
        String kw = "home";

        List<Attendance> attendances = attendanceRepository.findByDateAndWorkingFromLike(date, kw);

        // convert to DTO (AttendanceDTO has constructor from Attendance)
        List<com.erp.employee_service.dto.attendance.AttendanceDTO> dtos = attendances.stream()
                .map(att -> new com.erp.employee_service.dto.attendance.AttendanceDTO(att))
                .collect(Collectors.toList());

        return dtos;
    }

    /**
     * Return list of AttendanceDTO for employees who worked from home in the date range (inclusive).
     */
    @Transactional(readOnly = true)
    public List<com.erp.employee_service.dto.attendance.AttendanceDTO> getWorkFromHomeBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("from and to required");
        if (to.isBefore(from)) throw new IllegalArgumentException("to must not be before from");

        String kw = "home";
        List<Attendance> attendances = attendanceRepository.findByDateBetweenAndWorkingFromLike(from, to, kw);

        List<com.erp.employee_service.dto.attendance.AttendanceDTO> dtos = attendances.stream()
                .map(att -> new com.erp.employee_service.dto.attendance.AttendanceDTO(att))
                .collect(Collectors.toList());

        return dtos;
    }

    public AttendanceDTO getAttendanceBYId(Long attendanceId) {
        Optional<Attendance> attendance = attendanceRepository.findById(attendanceId);
        if (attendance.isEmpty()) {
            throw new IllegalArgumentException("Attendance not found with id: " + attendanceId);
        }

        return new AttendanceDTO(attendance.get());
    }
}