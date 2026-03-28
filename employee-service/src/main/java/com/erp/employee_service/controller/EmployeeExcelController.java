package com.erp.employee_service.controller;

import com.erp.employee_service.dto.imports.EmployeeImportRequest;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.service.EmployeeService;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.util.EmployeeExcelUtil;
import com.erp.employee_service.util.EmployeeCsvUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller for importing/exporting employees.
 *
 * - POST /employee/excel/import-csv  -> accepts .csv (minimal columns: employeeId,name,email,gender,joiningDate,mobile)
 * - POST /employee/excel/import-xlsx -> accepts .xlsx with same minimal columns (first sheet, header row)
 * - GET  /employee/excel/export     -> exports all employees to XLSX
 *
 * This controller delegates to employeeService.createEmployees(...) which should set safe defaults
 * for missing NOT-NULL DB columns (about, address, password, etc).
 */
@RestController
@RequestMapping("/employee/excel")
@Slf4j
@RequiredArgsConstructor
public class EmployeeExcelController {

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;

    /**
     * Import CSV (minimal fields).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importCsv(@RequestPart("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }

        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".csv")) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is not a CSV. Please upload a .csv file."));
        }

        List<EmployeeImportRequest> rows;
        try {
            rows = EmployeeCsvUtil.parseCsv(file);
        } catch (Exception ex) {
            log.error("Failed to parse CSV: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to parse CSV: " + ex.getMessage()));
        }

        if (rows.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No valid entries found in CSV"));
        }

        List<Object> results = new ArrayList<>();
        for (var row : rows) {
            try {
                // call minimal create method (must set safe defaults)
                Employee resp = employeeService.createEmployees(row);
                results.add(Map.of("employeeId", resp.getEmployeeId(), "status", "CREATED"));
            } catch (Exception ex) {
                log.warn("Import row failed (name={}): {}", row.getName(), ex.getMessage());
                results.add(Map.of("name", row.getName(), "error", ex.getMessage()));
            }
        }

        return ResponseEntity.ok(results);
    }

    /**
     * Export all employees to XLSX.
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = "employees-" + java.time.LocalDateTime.now().toString().replace(":", "-") + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        // Fetch all employees (or page, or filter)
        List<Employee> employees = employeeRepository.findAll();

        var in = EmployeeExcelUtil.employeesToExcel(employees);
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }
}
