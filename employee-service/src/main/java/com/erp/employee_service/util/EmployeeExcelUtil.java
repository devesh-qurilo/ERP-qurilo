package com.erp.employee_service.util;

import com.erp.employee_service.dto.CreateEmployeeRequest;
import com.erp.employee_service.entity.Employee;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility to read Employees from Excel and write Employees to Excel.
 * Assumes first row is header. Columns (order expected):
 * employeeId, name, email, gender, birthday, bloodGroup, joiningDate, language,
 * country, mobile, address, about, departmentId, designationId, reportingToId,
 * role, loginAllowed, receiveEmailNotification, hourlyRate, skills (comma sep),
 * probationEndDate, noticePeriodStartDate, noticePeriodEndDate, employmentType,
 * maritalStatus, businessAddress, officeShift, active
 */
public class EmployeeExcelUtil {

    public static final String[] HEADERS = new String[]{
            "employeeId", "name", "email", "gender", "birthday", "bloodGroup", "joiningDate", "language",
            "country", "mobile", "address", "about", "departmentId", "designationId", "reportingToId",
            "role", "loginAllowed", "receiveEmailNotification", "hourlyRate", "skills",
            "probationEndDate", "noticePeriodStartDate", "noticePeriodEndDate", "employmentType",
            "maritalStatus", "businessAddress", "officeShift", "active"
    };

    // ---------- READ ----------
    public static List<CreateEmployeeRequest> parseExcelFile(MultipartFile file) throws IOException {
        List<CreateEmployeeRequest> out = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> rows = sheet.iterator();
            if (!rows.hasNext()) return out; // empty

            // read header -> build index map to allow columns in any order
            Row headerRow = rows.next();
            Map<String, Integer> idx = new HashMap<>();
            for (Cell cell : headerRow) {
                String val = formatter.formatCellValue(cell).trim();
                if (!val.isEmpty()) idx.put(val, cell.getColumnIndex());
            }

            while (rows.hasNext()) {
                Row r = rows.next();
                // skip fully empty rows
                boolean empty = true;
                for (Cell c : r) {
                    if (!formatter.formatCellValue(c).trim().isEmpty()) {
                        empty = false; break;
                    }
                }
                if (empty) continue;

                CreateEmployeeRequest req = new CreateEmployeeRequest();
                req.setEmployeeId(getString(r, idx.get("employeeId"), formatter));
                req.setName(getString(r, idx.get("name"), formatter));
                req.setEmail(getString(r, idx.get("email"), formatter));
                String pwd = getString(r, idx.get("password"), formatter); // optional
                req.setPassword(pwd);
                req.setGender(getString(r, idx.get("gender"), formatter));
                req.setBirthday(getLocalDate(r, idx.get("birthday")));
                req.setBloodGroup(getString(r, idx.get("bloodGroup"), formatter));
                req.setJoiningDate(getLocalDate(r, idx.get("joiningDate")));
                req.setLanguage(getString(r, idx.get("language"), formatter));
                req.setCountry(getString(r, idx.get("country"), formatter));
                req.setMobile(getString(r, idx.get("mobile"), formatter));
                req.setAddress(getString(r, idx.get("address"), formatter));
                req.setAbout(getString(r, idx.get("about"), formatter));

                String deptId = getString(r, idx.get("departmentId"), formatter);
                if (deptId != null && !deptId.isBlank()) req.setDepartmentId(Long.valueOf(deptId));

                String desId = getString(r, idx.get("designationId"), formatter);
                if (desId != null && !desId.isBlank()) req.setDesignationId(Long.valueOf(desId));

                req.setReportingToId(getString(r, idx.get("reportingToId"), formatter));
                req.setRole(getString(r, idx.get("role"), formatter));
                String loginAllowed = getString(r, idx.get("loginAllowed"), formatter);
                if (loginAllowed != null && !loginAllowed.isBlank()) req.setLoginAllowed(Boolean.valueOf(loginAllowed));
                String receiveEmail = getString(r, idx.get("receiveEmailNotification"), formatter);
                if (receiveEmail != null && !receiveEmail.isBlank()) req.setReceiveEmailNotification(Boolean.valueOf(receiveEmail));
                String hourly = getString(r, idx.get("hourlyRate"), formatter);
                if (hourly != null && !hourly.isBlank()) req.setHourlyRate(Double.valueOf(hourly));
                String skills = getString(r, idx.get("skills"), formatter);
                if (skills != null && !skills.isBlank()) {
                    req.setSkills(Arrays.stream(skills.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList()));
                }
                req.setProbationEndDate(getLocalDate(r, idx.get("probationEndDate")));
                req.setNoticePeriodStartDate(getLocalDate(r, idx.get("noticePeriodStartDate")));
                req.setNoticePeriodEndDate(getLocalDate(r, idx.get("noticePeriodEndDate")));
                req.setEmploymentType(getString(r, idx.get("employmentType"), formatter));
                req.setMaritalStatus(getString(r, idx.get("maritalStatus"), formatter));
                req.setBusinessAddress(getString(r, idx.get("businessAddress"), formatter));
                req.setOfficeShift(getString(r, idx.get("officeShift"), formatter));
                String active = getString(r, idx.get("active"), formatter);
                if (active != null && !active.isBlank()) req.setLoginAllowed(Boolean.valueOf(active)); // optional mapping

                out.add(req);
            }
        }
        return out;
    }

    private static String getString(Row r, Integer col, DataFormatter f) {
        if (col == null) return null;
        Cell c = r.getCell(col);
        if (c == null) return null;
        return f.formatCellValue(c).trim();
    }

    private static LocalDate getLocalDate(Row r, Integer col) {
        if (col == null) return null;
        Cell c = r.getCell(col);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else {
            String raw = new DataFormatter().formatCellValue(c).trim();
            if (raw.isEmpty()) return null;
            try {
                return LocalDate.parse(raw);
            } catch (Exception ex) {
                // fallback: ignore parse error
                return null;
            }
        }
    }

    // ---------- WRITE ----------
    public static ByteArrayInputStream employeesToExcel(List<Employee> employees) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employees");
            // header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
            }

            int rowIdx = 1;
            for (Employee e : employees) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(e.getEmployeeId());
                row.createCell(c++).setCellValue(e.getName());
                row.createCell(c++).setCellValue(e.getEmail());
                row.createCell(c++).setCellValue(e.getGender());
                row.createCell(c++).setCellValue(e.getBirthday() != null ? e.getBirthday().toString() : "");
                row.createCell(c++).setCellValue(e.getBloodGroup() != null ? e.getBloodGroup() : "");
                row.createCell(c++).setCellValue(e.getJoiningDate() != null ? e.getJoiningDate().toString() : "");
                row.createCell(c++).setCellValue(e.getLanguage() != null ? e.getLanguage() : "");
                row.createCell(c++).setCellValue(e.getCountry() != null ? e.getCountry() : "");
                row.createCell(c++).setCellValue(e.getMobile() != null ? e.getMobile() : "");
                row.createCell(c++).setCellValue(e.getAddress() != null ? e.getAddress() : "");
                row.createCell(c++).setCellValue(e.getAbout() != null ? e.getAbout() : "");
                row.createCell(c++).setCellValue(e.getDepartment() != null ? String.valueOf(e.getDepartment().getId()) : "");
                row.createCell(c++).setCellValue(e.getDesignation() != null ? String.valueOf(e.getDesignation().getId()) : "");
                row.createCell(c++).setCellValue(e.getReportingTo() != null ? e.getReportingTo().getEmployeeId() : "");
                row.createCell(c++).setCellValue(e.getRole() != null ? e.getRole() : "");
                row.createCell(c++).setCellValue(e.getLoginAllowed() != null ? e.getLoginAllowed().toString() : "");
                row.createCell(c++).setCellValue(e.getReceiveEmailNotification() != null ? e.getReceiveEmailNotification().toString() : "");
                row.createCell(c++).setCellValue(e.getHourlyRate() != null ? e.getHourlyRate().toString() : "");
                row.createCell(c++).setCellValue(e.getSkills() != null ? String.join(",", e.getSkills()) : "");
                row.createCell(c++).setCellValue(e.getProbationEndDate() != null ? e.getProbationEndDate().toString() : "");
                row.createCell(c++).setCellValue(e.getNoticePeriodStartDate() != null ? e.getNoticePeriodStartDate().toString() : "");
                row.createCell(c++).setCellValue(e.getNoticePeriodEndDate() != null ? e.getNoticePeriodEndDate().toString() : "");
                row.createCell(c++).setCellValue(e.getEmploymentType() != null ? e.getEmploymentType() : "");
                row.createCell(c++).setCellValue(e.getMaritalStatus() != null ? e.getMaritalStatus() : "");
                row.createCell(c++).setCellValue(e.getBusinessAddress() != null ? e.getBusinessAddress() : "");
                row.createCell(c++).setCellValue(e.getOfficeShift() != null ? e.getOfficeShift() : "");
                row.createCell(c++).setCellValue(e.getActive() != null ? e.getActive().toString() : "");
            }

            // autosize columns (optional)
            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
