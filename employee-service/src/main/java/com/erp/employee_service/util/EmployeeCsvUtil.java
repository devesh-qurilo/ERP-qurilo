package com.erp.employee_service.util;

import com.erp.employee_service.dto.imports.EmployeeImportRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple CSV parser for the minimal import template:
 * Columns expected (header): employeeId,name,email,gender,joiningDate,mobile
 *
 * NOTE: This is a minimal parser. It splits by comma and does NOT fully support
 * quoted commas. For robust CSV handling, use Apache Commons CSV or OpenCSV.
 */
public class EmployeeCsvUtil {

    public static List<EmployeeImportRequest> parseCsv(MultipartFile file) throws Exception {
        List<EmployeeImportRequest> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) return out;
            String[] headers = headerLine.split(",");
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                idx.put(headers[i].trim(), i);
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",", -1); // keep empty trailing columns

                EmployeeImportRequest req = new EmployeeImportRequest();
                req.setEmployeeId(get(cols, idx, "employeeId"));
                req.setName(get(cols, idx, "name"));
                req.setEmail(get(cols, idx, "email"));
                req.setGender(get(cols, idx, "gender"));

                String joiningRaw = get(cols, idx, "joiningDate");
                if (joiningRaw != null && !joiningRaw.isBlank()) {
                    try {
                        req.setJoiningDate(LocalDate.parse(joiningRaw));
                    } catch (Exception ex) {
                        // ignore parse error -> null
                    }
                }
                req.setMobile(get(cols, idx, "mobile"));

                // Only add rows that have a name and email (minimal validation)
                if (req.getName() != null && !req.getName().isBlank()
                        && req.getEmail() != null && !req.getEmail().isBlank()) {
                    out.add(req);
                }
            }
        }
        return out;
    }

    private static String get(String[] cols, Map<String, Integer> idx, String name) {
        Integer i = idx.get(name);
        if (i == null) return null;
        if (i < 0 || i >= cols.length) return null;
        String v = cols[i].trim();
        return v.isEmpty() ? null : v;
    }
}
