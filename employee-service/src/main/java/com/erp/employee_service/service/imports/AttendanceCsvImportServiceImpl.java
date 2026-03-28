//package com.erp.employee_service.service.imports;
//
//import com.erp.employee_service.dto.imports.AttendanceImport;
//import com.erp.employee_service.dto.imports.ImportResult;
//import com.erp.employee_service.entity.Employee;
//import com.erp.employee_service.entity.attendance.Attendance;
//import com.erp.employee_service.repository.AttendanceRepository;
//import com.erp.employee_service.repository.EmployeeRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AttendanceCsvImportServiceImpl implements AttendanceCsvImportService {
//
//    private final AttendanceRepository attendanceRepository;
//    private final EmployeeRepository employeeRepository;
//
//    @Override
//    @Transactional
//    public List<ImportResult> importAttendanceFromCsv(MultipartFile file, String markedBy, boolean overwrite) {
//        List<ImportResult> results = new ArrayList<>();
//
//        if (file == null || file.isEmpty()) {
//            results.add(new ImportResult(0, "ERROR", "Empty or missing file", null));
//            return results;
//        }
//
//        try (BufferedReader reader = new BufferedReader(
//                new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
//
//            String line;
//            List<String> headers = new ArrayList<>();
//            int rowNumber = 0;
//
//            // Read file line by line
//            while ((line = reader.readLine()) != null) {
//                rowNumber++;
//
//                // Skip empty lines
//                if (line.trim().isEmpty()) continue;
//
//                String[] fields = parseCsvLine(line);
//
//                // First line is header
//                if (rowNumber == 1) {
//                    headers = Arrays.asList(fields);
//                    continue;
//                }
//
//                // Process data rows
//                try {
//                    AttendanceImport imp = mapFieldsToAttendanceImport(fields, headers);
//                    ImportResult result = processAttendanceRow(imp, rowNumber, markedBy, overwrite);
//                    results.add(result);
//
//                } catch (Exception exRow) {
//                    log.error("Error importing row {}: {}", rowNumber, exRow.getMessage(), exRow);
//                    results.add(new ImportResult(rowNumber, "ERROR", "Unhandled: " + safeMessage(exRow), null));
//                }
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to parse CSV file for attendance import: {}", e.getMessage(), e);
//            results.add(new ImportResult(0, "ERROR", "Failed to parse file: " + safeMessage(e), null));
//        }
//
//        return results;
//    }
//
//    private String[] parseCsvLine(String line) {
//        List<String> fields = new ArrayList<>();
//        StringBuilder currentField = new StringBuilder();
//        boolean inQuotes = false;
//
//        for (int i = 0; i < line.length(); i++) {
//            char c = line.charAt(i);
//
//            if (c == '"') {
//                inQuotes = !inQuotes;
//            } else if (c == ',' && !inQuotes) {
//                fields.add(currentField.toString().trim());
//                currentField.setLength(0);
//            } else {
//                currentField.append(c);
//            }
//        }
//
//        // Add the last field
//        fields.add(currentField.toString().trim());
//
//        return fields.toArray(new String[0]);
//    }
//
//    private AttendanceImport mapFieldsToAttendanceImport(String[] fields, List<String> headers) {
//        AttendanceImport imp = new AttendanceImport();
//        Map<String, String> headerMap = new HashMap<>();
//
//        // Create normalized header map
//        for (int i = 0; i < headers.size(); i++) {
//            String normalized = normalizeHeaderKey(headers.get(i));
//            headerMap.put(normalized, headers.get(i));
//        }
//
//        // Map fields to object
//        for (int i = 0; i < Math.min(fields.length, headers.size()); i++) {
//            String header = headers.get(i);
//            String value = fields[i];
//            String normalizedHeader = normalizeHeaderKey(header);
//
//            switch (normalizedHeader) {
//                case "employeeid":
//                case "empid":
//                case "employee":
//                    imp.setEmployeeId(value);
//                    break;
//                case "date":
//                case "attendancedate":
//                case "day":
//                    imp.setDate(value);
//                    break;
//                case "clockintime":
//                case "intime":
//                case "clock_in":
//                    imp.setClockInTime(value);
//                    break;
//                case "clockouttime":
//                case "outtime":
//                case "clock_out":
//                    imp.setClockOutTime(value);
//                    break;
//                case "clockinlocation":
//                case "inlocation":
//                    imp.setClockInLocation(value);
//                    break;
//                case "clockoutlocation":
//                case "outlocation":
//                    imp.setClockOutLocation(value);
//                    break;
//                case "clockinworkingfrom":
//                case "inworkingfrom":
//                    imp.setClockInWorkingFrom(value);
//                    break;
//                case "clockoutworkingfrom":
//                case "outworkingfrom":
//                    imp.setClockOutWorkingFrom(value);
//                    break;
//                case "late":
//                case "islate":
//                    imp.setLate(value);
//                    break;
//                case "halfday":
//                case "ishalfday":
//                    imp.setHalfDay(value);
//                    break;
//            }
//        }
//
//        return imp;
//    }
//
//    private ImportResult processAttendanceRow(AttendanceImport imp, int rowNumber, String markedBy, boolean overwrite) {
//        // Required fields validation
//        String empId = safeTrim(imp.getEmployeeId());
//        String dateRaw = safeTrim(imp.getDate());
//
//        if (empId == null || empId.isBlank()) {
//            return new ImportResult(rowNumber, "SKIPPED", "Missing employeeId", null);
//        }
//        if (dateRaw == null || dateRaw.isBlank()) {
//            return new ImportResult(rowNumber, "SKIPPED", "Missing date", null);
//        }
//
//        // Find employee
//        Optional<Employee> empOpt = employeeRepository.findById(empId);
//        if (empOpt.isEmpty()) {
//            return new ImportResult(rowNumber, "SKIPPED", "Employee not found: " + empId, null);
//        }
//        Employee emp = empOpt.get();
//
//        // Parse date
//        LocalDate date = parseFlexibleDate(dateRaw);
//        if (date == null) {
//            return new ImportResult(rowNumber, "ERROR", "Invalid date format: " + dateRaw, null);
//        }
//
//        // Parse times
//        LocalTime clockIn = parseFlexibleTime(safeTrim(imp.getClockInTime()));
//        LocalTime clockOut = parseFlexibleTime(safeTrim(imp.getClockOutTime()));
//
//        // Parse flags
//        Boolean late = parseBooleanFlexible(imp.getLate());
//        Boolean halfDay = parseBooleanFlexible(imp.getHalfDay());
//
//        // Duplicate check
//        Optional<Attendance> existingOpt = attendanceRepository.findByEmployeeAndDate(emp, date);
//
//        if (existingOpt.isPresent() && !overwrite) {
//            return new ImportResult(rowNumber, "SKIPPED", "Attendance exists and overwrite=false", null);
//        }
//
//        Attendance at;
//        if (existingOpt.isPresent()) {
//            at = existingOpt.get();
//        } else {
//            at = Attendance.builder()
//                    .employee(emp)
//                    .date(date)
//                    .build();
//        }
//
//        // Apply values
//        if (clockIn != null) at.setClockInTime(clockIn);
//        if (clockOut != null) at.setClockOutTime(clockOut);
//        if (imp.getClockInLocation() != null) at.setClockInLocation(safeTrim(imp.getClockInLocation()));
//        if (imp.getClockOutLocation() != null) at.setClockOutLocation(safeTrim(imp.getClockOutLocation()));
//        if (imp.getClockInWorkingFrom() != null) at.setClockInWorkingFrom(safeTrim(imp.getClockInWorkingFrom()));
//        if (imp.getClockOutWorkingFrom() != null) at.setClockOutWorkingFrom(safeTrim(imp.getClockOutWorkingFrom()));
//        if (late != null) at.setLate(late);
//        if (halfDay != null) at.setHalfDay(halfDay);
//
//        at.setOverwritten(overwrite || Boolean.TRUE.equals(at.getOverwritten()));
//
//        if (overwrite) {
//            at.setIsPresent(Boolean.TRUE);
//        } else if (at.getClockInTime() != null || at.getClockOutTime() != null) {
//            at.setIsPresent(Boolean.TRUE);
//        }
//
//        if (markedBy != null && !markedBy.isBlank()) {
//            employeeRepository.findById(markedBy).ifPresent(at::setMarkedBy);
//        }
//
//        Attendance saved = attendanceRepository.save(at);
//        return new ImportResult(rowNumber, "CREATED", null, saved.getId());
//    }
//
//    // Helper methods
//    private static String normalizeHeaderKey(String h) {
//        if (h == null) return "";
//        return h.trim().toLowerCase().replaceAll("\\s+", "").replaceAll("_", "");
//    }
//
//    private static String safeTrim(String s) {
//        return s == null ? null : s.trim();
//    }
//
//    private static String safeMessage(Exception e) {
//        if (e == null) return "";
//        return e.getMessage() == null ? e.toString() : e.getMessage();
//    }
//
//    private LocalDate parseFlexibleDate(String input) {
//        if (input == null || input.isBlank()) return null;
//
//        List<String> patterns = List.of(
//                "yyyy-MM-dd",
//                "dd-MM-yyyy",
//                "dd/MM/yyyy",
//                "MM/dd/yyyy"
//        );
//
//        for (String p : patterns) {
//            try {
//                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(p);
//                return LocalDate.parse(input, fmt);
//            } catch (Exception ignored) {}
//        }
//
//        try {
//            return LocalDate.parse(input);
//        } catch (Exception ignored) {}
//
//        return null;
//    }
//
//    private LocalTime parseFlexibleTime(String input) {
//        if (input == null || input.isBlank()) return null;
//        input = input.trim();
//
//        List<DateTimeFormatter> formats = List.of(
//                DateTimeFormatter.ofPattern("H:mm"),
//                DateTimeFormatter.ofPattern("HH:mm"),
//                DateTimeFormatter.ofPattern("H:mm:ss"),
//                DateTimeFormatter.ofPattern("hh:mm a"),
//                DateTimeFormatter.ofPattern("h:mm a"),
//                DateTimeFormatter.ISO_TIME
//        );
//
//        for (DateTimeFormatter fmt : formats) {
//            try {
//                return LocalTime.parse(input, fmt);
//            } catch (Exception ignored) {}
//        }
//
//        try {
//            String digits = input.replaceAll("\\D+", "");
//            if (digits.length() == 4) {
//                int hh = Integer.parseInt(digits.substring(0, 2));
//                int mm = Integer.parseInt(digits.substring(2, 4));
//                return LocalTime.of(hh, mm);
//            }
//        } catch (Exception ignored) {}
//
//        return null;
//    }
//
//    private Boolean parseBooleanFlexible(String s) {
//        if (s == null) return null;
//        s = s.trim().toLowerCase();
//        if (s.isEmpty()) return null;
//        if (s.equals("true") || s.equals("yes") || s.equals("1") || s.equals("y")) return Boolean.TRUE;
//        if (s.equals("false") || s.equals("no") || s.equals("0") || s.equals("n")) return Boolean.FALSE;
//        return null;
//    }
//}

package com.erp.employee_service.service.imports;

import com.erp.employee_service.dto.imports.AttendanceImport;
import com.erp.employee_service.dto.imports.ImportResult;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.attendance.Attendance;
import com.erp.employee_service.repository.AttendanceRepository;
import com.erp.employee_service.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceCsvImportServiceImpl implements AttendanceCsvImportService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public List<ImportResult> importAttendanceFromCsv(MultipartFile file, String markedBy, boolean overwrite) {
        List<ImportResult> results = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            results.add(new ImportResult(0, "ERROR", "Empty or missing file", null));
            return results;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

            String line;
            List<String> headers = new ArrayList<>();
            int rowNumber = 0;

            // Read file line by line
            while ((line = reader.readLine()) != null) {
                rowNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                String[] fields = parseCsvLine(line);

                // First line is header
                if (rowNumber == 1) {
                    headers = Arrays.asList(fields);
                    continue;
                }

                // Process data rows
                try {
                    AttendanceImport imp = mapFieldsToAttendanceImport(fields, headers);
                    ImportResult result = processAttendanceRow(imp, rowNumber, markedBy, overwrite);
                    results.add(result);

                } catch (Exception exRow) {
                    log.error("Error importing row {}: {}", rowNumber, exRow.getMessage(), exRow);
                    results.add(new ImportResult(rowNumber, "ERROR", "Unhandled: " + safeMessage(exRow), null));
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse CSV file for attendance import: {}", e.getMessage(), e);
            results.add(new ImportResult(0, "ERROR", "Failed to parse file: " + safeMessage(e), null));
        }

        return results;
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }

        // Add the last field
        fields.add(currentField.toString().trim());

        return fields.toArray(new String[0]);
    }

    private AttendanceImport mapFieldsToAttendanceImport(String[] fields, List<String> headers) {
        AttendanceImport imp = new AttendanceImport();

        // Create header mapping with all possible variations (like your original code)
        Map<String, Integer> headerIndexMap = createHeaderIndexMap(headers);

        // Map fields using flexible header matching
        imp.setEmployeeId(getFieldValue(fields, headerIndexMap,
                new String[]{"employeeid", "employee id", "empid", "emp id", "employee", "emp_id", "employee_id"}));

        imp.setDate(getFieldValue(fields, headerIndexMap,
                new String[]{"date", "attendance date", "day", "attendance_date"}));

        imp.setClockInTime(getFieldValue(fields, headerIndexMap,
                new String[]{"clockintime", "clock in time", "in time", "clock_in_time", "clock_in", "intime", "checkin", "check_in"}));

        imp.setClockOutTime(getFieldValue(fields, headerIndexMap,
                new String[]{"clockouttime", "clock out time", "out time", "clock_out_time", "clock_out", "outtime", "checkout", "check_out"}));

        imp.setClockInLocation(getFieldValue(fields, headerIndexMap,
                new String[]{"clock_in_location", "clock in location", "in location", "in_location", "checkinlocation", "check_in_location"}));

        imp.setClockOutLocation(getFieldValue(fields, headerIndexMap,
                new String[]{"clock_out_location", "clock out location", "out location", "out_location", "checkoutlocation", "check_out_location"}));

        imp.setClockInWorkingFrom(getFieldValue(fields, headerIndexMap,
                new String[]{"clock_in_working_from", "working from in", "working_from_in", "in_working_from", "workfrom_in", "work_from_in"}));

        imp.setClockOutWorkingFrom(getFieldValue(fields, headerIndexMap,
                new String[]{"clock_out_working_from", "working from out", "working_from_out", "out_working_from", "workfrom_out", "work_from_out"}));

        imp.setLate(getFieldValue(fields, headerIndexMap,
                new String[]{"late", "is_late", "is late", "late_mark", "late_status"}));

        imp.setHalfDay(getFieldValue(fields, headerIndexMap,
                new String[]{"halfday", "half day", "is_half_day", "is half day", "half_day", "halfday_status"}));

        return imp;
    }

    private Map<String, Integer> createHeaderIndexMap(List<String> headers) {
        Map<String, Integer> headerIndexMap = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header != null) {
                // Add all possible normalized versions
                String normalized = normalizeHeaderKey(header);
                headerIndexMap.put(normalized, i);

                // Also add without special characters
                String simple = header.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                headerIndexMap.put(simple, i);
            }
        }

        return headerIndexMap;
    }

    private String getFieldValue(String[] fields, Map<String, Integer> headerIndexMap, String[] possibleHeaders) {
        for (String header : possibleHeaders) {
            String normalized = normalizeHeaderKey(header);
            Integer index = headerIndexMap.get(normalized);
            if (index != null && index < fields.length) {
                String value = fields[index];
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private ImportResult processAttendanceRow(AttendanceImport imp, int rowNumber, String markedBy, boolean overwrite) {
        // Required fields validation
        String empId = safeTrim(imp.getEmployeeId());
        String dateRaw = safeTrim(imp.getDate());

        if (empId == null || empId.isBlank()) {
            return new ImportResult(rowNumber, "SKIPPED", "Missing employeeId", null);
        }
        if (dateRaw == null || dateRaw.isBlank()) {
            return new ImportResult(rowNumber, "SKIPPED", "Missing date", null);
        }

        // Find employee
        Optional<Employee> empOpt = employeeRepository.findById(empId);
        if (empOpt.isEmpty()) {
            return new ImportResult(rowNumber, "SKIPPED", "Employee not found: " + empId, null);
        }
        Employee emp = empOpt.get();

        // Parse date with flexible formats
        LocalDate date = parseFlexibleDate(dateRaw);
        if (date == null) {
            return new ImportResult(rowNumber, "ERROR", "Invalid date format: " + dateRaw + ". Supported formats: yyyy-MM-dd, dd-MM-yyyy, dd/MM/yyyy, MM/dd/yyyy", null);
        }

        // Parse times with flexible formats
        LocalTime clockIn = parseFlexibleTime(safeTrim(imp.getClockInTime()));
        LocalTime clockOut = parseFlexibleTime(safeTrim(imp.getClockOutTime()));

        // Parse flags with flexible boolean values
        Boolean late = parseBooleanFlexible(imp.getLate());
        Boolean halfDay = parseBooleanFlexible(imp.getHalfDay());

        // Duplicate check
        Optional<Attendance> existingOpt = attendanceRepository.findByEmployeeAndDate(emp, date);

        if (existingOpt.isPresent() && !overwrite) {
            return new ImportResult(rowNumber, "SKIPPED", "Attendance exists and overwrite=false", null);
        }

        Attendance at;
        if (existingOpt.isPresent()) {
            at = existingOpt.get();
            log.info("Updating existing attendance for employee {} on date {}", empId, date);
        } else {
            at = Attendance.builder()
                    .employee(emp)
                    .date(date)
                    .build();
            log.info("Creating new attendance for employee {} on date {}", empId, date);
        }

        // Apply values (only if provided - like your original logic)
        if (clockIn != null) {
            at.setClockInTime(clockIn);
            log.debug("Set clock in time: {}", clockIn);
        }
        if (clockOut != null) {
            at.setClockOutTime(clockOut);
            log.debug("Set clock out time: {}", clockOut);
        }
        if (imp.getClockInLocation() != null && !imp.getClockInLocation().trim().isEmpty()) {
            at.setClockInLocation(safeTrim(imp.getClockInLocation()));
        }
        if (imp.getClockOutLocation() != null && !imp.getClockOutLocation().trim().isEmpty()) {
            at.setClockOutLocation(safeTrim(imp.getClockOutLocation()));
        }
        if (imp.getClockInWorkingFrom() != null && !imp.getClockInWorkingFrom().trim().isEmpty()) {
            at.setClockInWorkingFrom(safeTrim(imp.getClockInWorkingFrom()));
        }
        if (imp.getClockOutWorkingFrom() != null && !imp.getClockOutWorkingFrom().trim().isEmpty()) {
            at.setClockOutWorkingFrom(safeTrim(imp.getClockOutWorkingFrom()));
        }
        if (late != null) {
            at.setLate(late);
        }
        if (halfDay != null) {
            at.setHalfDay(halfDay);
        }

        // Overwrite logic
        at.setOverwritten(overwrite || Boolean.TRUE.equals(at.getOverwritten()));

        // Determine isPresent
        if (overwrite) {
            at.setIsPresent(Boolean.TRUE);
        } else if (at.getClockInTime() != null || at.getClockOutTime() != null) {
            at.setIsPresent(Boolean.TRUE);
        }

        // markedBy
        if (markedBy != null && !markedBy.isBlank()) {
            employeeRepository.findById(markedBy).ifPresent(at::setMarkedBy);
        }

        Attendance saved = attendanceRepository.save(at);
        return new ImportResult(rowNumber, existingOpt.isPresent() ? "UPDATED" : "CREATED", null, saved.getId());
    }

    // Helper methods - Same as your original
    private static String normalizeHeaderKey(String h) {
        if (h == null) return "";
        return h.trim().toLowerCase().replaceAll("\\s+", "").replaceAll("_", "");
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static String safeMessage(Exception e) {
        if (e == null) return "";
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    // Flexible date parser - Same as your original
    private LocalDate parseFlexibleDate(String input) {
        if (input == null || input.isBlank()) return null;

        List<String> patterns = List.of(
                "yyyy-MM-dd",
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "yyyy/MM/dd"
        );

        for (String p : patterns) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(p);
                return LocalDate.parse(input, fmt);
            } catch (Exception ignored) {}
        }

        // Try ISO parser as last resort
        try {
            return LocalDate.parse(input);
        } catch (Exception ignored) {}

        return null;
    }

    // Flexible time parser - Same as your original
    private LocalTime parseFlexibleTime(String input) {
        if (input == null || input.isBlank()) return null;
        input = input.trim();

        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:mm:ss"),
                DateTimeFormatter.ofPattern("HH:mm:ss"),
                DateTimeFormatter.ofPattern("hh:mm a"),
                DateTimeFormatter.ofPattern("h:mm a"),
                DateTimeFormatter.ISO_TIME
        );

        for (DateTimeFormatter fmt : formats) {
            try {
                return LocalTime.parse(input, fmt);
            } catch (Exception ignored) {}
        }

        // try extracting digits like "0930" -> 09:30
        try {
            String digits = input.replaceAll("\\D+", "");
            if (digits.length() == 4) {
                int hh = Integer.parseInt(digits.substring(0, 2));
                int mm = Integer.parseInt(digits.substring(2, 4));
                if (hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59) {
                    return LocalTime.of(hh, mm);
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    // Flexible boolean parser - Same as your original
    private Boolean parseBooleanFlexible(String s) {
        if (s == null) return null;
        s = s.trim().toLowerCase();
        if (s.isEmpty()) return null;
        if (s.equals("true") || s.equals("yes") || s.equals("1") || s.equals("y") || s.equals("yes")) return Boolean.TRUE;
        if (s.equals("false") || s.equals("no") || s.equals("0") || s.equals("n") || s.equals("no")) return Boolean.FALSE;
        return null;
    }
}