//package com.erp.project_service.service.impl;
//
//import com.erp.project_service.dto.Import.ImportResult;
//import com.erp.project_service.dto.Import.ProjectImport;
//import com.erp.project_service.entity.Project;
//import com.erp.project_service.repository.ProjectRepository;
//import com.erp.project_service.service.interfaces.ProjectCsvImportService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.csv.CSVFormat;
//import org.apache.commons.csv.CSVParser;
//import org.apache.commons.csv.CSVRecord;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ProjectCsvImportServiceImpl implements ProjectCsvImportService {
//
//    private final ProjectRepository projectRepository;
//
//    @Override
//    @Transactional
//    public List<ImportResult> importProjectsFromCsv(MultipartFile file, String actorId) {
//        List<ImportResult> results = new ArrayList<>();
//        if (file == null || file.isEmpty()) {
//            results.add(new ImportResult(0, "ERROR", "Empty or missing file", null));
//            return results;
//        }
//
//        try (InputStream in = file.getInputStream();
//             InputStreamReader reader = new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
//
//            CSVParser parser = CSVFormat.DEFAULT
//                    .withFirstRecordAsHeader()
//                    .withTrim(false)
//                    .parse(reader);
//
//            Map<String, String> headerMap = new HashMap<>();
//            for (String h : parser.getHeaderMap().keySet()) {
//                if (h != null) headerMap.put(normalizeHeaderKey(h), h);
//            }
//
//            for (CSVRecord record : parser) {
//                int rowNum = (int) record.getRecordNumber() + 1;
//                try {
//                    ProjectImport imp = mapRecordToProjectImport(record, headerMap);
//
//                    // Validate required fields
//                    String shortCode = safeTrim(imp.getShortCode());
//                    String name = safeTrim(imp.getName());
//                    String clientId = safeTrim(imp.getClientId());
//
//                    if (shortCode == null || shortCode.isBlank()) {
//                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing shortCode", null));
//                        continue;
//                    }
//                    if (name == null || name.isBlank()) {
//                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing name", null));
//                        continue;
//                    }
//                    if (clientId == null || clientId.isBlank()) {
//                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing clientId", null));
//                        continue;
//                    }
//
//                    // Duplicate check by shortCode
//                    if (projectRepository.existsByShortCode(shortCode)) {
//                        results.add(new ImportResult(rowNum, "SKIPPED", "Duplicate shortCode: " + shortCode, null));
//                        continue;
//                    }
//
//                    // Parse dates
//                    LocalDate startDate = null;
//                    LocalDate deadline = null;
//                    if (imp.getStartDate() != null && !imp.getStartDate().isBlank()) {
//                        startDate = parseFlexibleDate(imp.getStartDate().trim());
//                        if (startDate == null) {
//                            results.add(new ImportResult(rowNum, "ERROR", "Invalid startDate: " + imp.getStartDate(), null));
//                            continue;
//                        }
//                    }
//                    if (imp.getDeadline() != null && !imp.getDeadline().isBlank()) {
//                        deadline = parseFlexibleDate(imp.getDeadline().trim());
//                        if (deadline == null) {
//                            results.add(new ImportResult(rowNum, "ERROR", "Invalid deadline: " + imp.getDeadline(), null));
//                            continue;
//                        }
//                    }
//
//                    // Parse budget
//                    BigDecimal budget = null;
//                    if (imp.getBudget() != null && !imp.getBudget().isBlank()) {
//                        try {
//                            String num = imp.getBudget().replaceAll("[^0-9.\\-]", "");
//                            if (!num.isBlank()) budget = new BigDecimal(num);
//                        } catch (Exception e) {
//                            results.add(new ImportResult(rowNum, "ERROR", "Invalid budget: " + imp.getBudget(), null));
//                            continue;
//                        }
//                    }
//
//                    // Parse assignedEmployeeIds -> Set<String>
//                    Set<String> assigned = new HashSet<>();
//                    if (imp.getAssignedEmployeeIds() != null && !imp.getAssignedEmployeeIds().isBlank()) {
//                        String cleaned = imp.getAssignedEmployeeIds().replaceAll("[\\[\\]\"]", "");
//                        String[] parts = cleaned.split(",");
//                        for (String p : parts) {
//                            String t = p.trim();
//                            if (!t.isEmpty()) assigned.add(t);
//                        }
//                    }
//
//                    // Build Project entity and save (directly, no notification/file upload)
//                    Project p = Project.builder()
//                            .shortCode(shortCode)
//                            .name(name)
//                            .startDate(startDate)
//                            .deadline(deadline)
//                            .clientId(clientId)
//                            .budget(budget)
//                            .assignedEmployeeIds(assigned)
//                            .build();
//
//                    // ensure audit fields if BaseAuditable requires them (createdBy etc.)
//                    try { p.setCreatedBy(actorId); } catch (Exception ignored) {}
//                    try { p.setAddedBy(actorId); } catch (Exception ignored) {}
//
//                    Project saved = projectRepository.save(p);
//                    results.add(new ImportResult(rowNum, "CREATED", null, saved.getId()));
//
//                } catch (Exception exRow) {
//                    log.error("Error importing row {}: {}", record.getRecordNumber(), exRow.getMessage(), exRow);
//                    results.add(new ImportResult(rowNum, "ERROR", "Unhandled: " + safeMessage(exRow), null));
//                }
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to parse CSV file for import: {}", e.getMessage(), e);
//            results.add(new ImportResult(0, "ERROR", "Failed to parse file: " + safeMessage(e), null));
//        }
//
//        return results;
//    }
//
//    // ----------------- helpers -----------------
//    private static String normalizeHeaderKey(String h) {
//        if (h == null) return "";
//        return h.trim().toLowerCase().replaceAll("\\s+", "");
//    }
//
//    private static String safeTrim(String s) {
//        return s == null ? null : s.trim();
//    }
//
//    private ProjectImport mapRecordToProjectImport(CSVRecord rec, Map<String, String> headerMap) {
//        ProjectImport imp = new ProjectImport();
//
//        java.util.function.Function<String[], String> getVal = (keys) -> {
//            for (String k : keys) {
//                String nk = normalizeHeaderKey(k);
//                String actual = headerMap.get(nk);
//                if (actual != null && rec.isMapped(actual)) {
//                    String v = rec.get(actual);
//                    if (v != null) return v.trim();
//                }
//            }
//            return null;
//        };
//
//        imp.setShortCode(getVal.apply(new String[]{"shortcode","short code","code"}));
//        imp.setName(getVal.apply(new String[]{"name","project name","title"}));
//        imp.setStartDate(getVal.apply(new String[]{"startdate","start date","start"}));
//        imp.setDeadline(getVal.apply(new String[]{"deadline","enddate","end date","due date"}));
//        imp.setClientId(getVal.apply(new String[]{"clientid","client id","client"}));
//        imp.setBudget(getVal.apply(new String[]{"budget","projectbudget","project budget","amount"}));
//        imp.setAssignedEmployeeIds(getVal.apply(new String[]{"assignedemployeeids","assigned employee ids","assigned","assignedEmployeeIds","assigned_ids"}));
//
//        return imp;
//    }
//
//    private static String safeMessage(Exception e) {
//        if (e == null) return "";
//        return e.getMessage() == null ? e.toString() : e.getMessage();
//    }
//
//    // Flexible date parser same as earlier
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
//        for (String pattern : patterns) {
//            try {
//                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
//                return LocalDate.parse(input, fmt);
//            } catch (Exception ignored) {}
//        }
//        return null;
//    }
//}
package com.erp.project_service.service.impl;

import com.erp.project_service.dto.Import.ImportResult;
import com.erp.project_service.dto.Import.ProjectImport;
import com.erp.project_service.entity.Project;
import com.erp.project_service.repository.ProjectRepository;
import com.erp.project_service.service.interfaces.ProjectCsvImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectCsvImportServiceImpl implements ProjectCsvImportService {

    private final ProjectRepository projectRepository;

    // Required header normalized keys synonyms
    private static final List<String> REQUIRED_KEYS_SYNONYMS_SHORTCODE = List.of("shortcode","short code","code");
    private static final List<String> REQUIRED_KEYS_SYNONYMS_NAME = List.of("name","project name","title");
    private static final List<String> REQUIRED_KEYS_SYNONYMS_CLIENT = List.of("clientid","client id","client");

    @Override
    @Transactional
    public List<ImportResult> importProjectsFromCsv(MultipartFile file, String actorId) {
        List<ImportResult> results = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            results.add(new ImportResult(0, "ERROR", "Empty or missing file", null));
            return results;
        }

        try (InputStream in = file.getInputStream();
             InputStreamReader reader = new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {

            // IMPORTANT: allow missing column names so parser doesn't throw on trailing commas / blank headers
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim(false)
                    .withAllowMissingColumnNames()   // <<-- prevents "A header name is missing" exception
                    .parse(reader);

            // ---- Robust header map build: ignore blank headers, detect duplicate normalized keys ----
            Map<String, String> headerMap = new HashMap<>();
            for (String rawHeader : parser.getHeaderMap().keySet()) {
                if (rawHeader == null) continue;
                String normalized = normalizeHeaderKey(rawHeader);
                // skip completely empty header columns (e.g., trailing comma → empty header)
                if (normalized.isBlank()) {
                    log.debug("Ignoring blank CSV header column (raw header='{}')", rawHeader);
                    continue;
                }
                if (headerMap.containsKey(normalized)) {
                    // warn and keep first occurrence
                    log.warn("Duplicate normalized CSV header '{}' found (keeping first: '{}' ; ignoring: '{}')",
                            normalized, headerMap.get(normalized), rawHeader);
                    continue;
                }
                headerMap.put(normalized, rawHeader);
            }

            // --- REQUIREMENT: ensure required headers exist in the file (in any supported synonym) ---
            Optional<String> shortCodeHeader = findFirstHeaderForSynonyms(headerMap, REQUIRED_KEYS_SYNONYMS_SHORTCODE);
            Optional<String> nameHeader = findFirstHeaderForSynonyms(headerMap, REQUIRED_KEYS_SYNONYMS_NAME);
            Optional<String> clientHeader = findFirstHeaderForSynonyms(headerMap, REQUIRED_KEYS_SYNONYMS_CLIENT);

            if (shortCodeHeader.isEmpty() || nameHeader.isEmpty() || clientHeader.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                if (shortCodeHeader.isEmpty()) sb.append("Missing required header: shortCode. ");
                if (nameHeader.isEmpty()) sb.append("Missing required header: name. ");
                if (clientHeader.isEmpty()) sb.append("Missing required header: clientId. ");
                results.add(new ImportResult(0, "ERROR", sb.toString().trim(), null));
                return results;
            }

            // For mapping convenience, create effective map only for supported keys (required + optionals)
            Map<String, String> effectiveMap = new HashMap<>();
            effectiveMap.put("shortcode", headerMap.get(normalizeHeaderKey(shortCodeHeader.get())));
            effectiveMap.put("name", headerMap.get(normalizeHeaderKey(nameHeader.get())));
            effectiveMap.put("clientid", headerMap.get(normalizeHeaderKey(clientHeader.get())));
            putIfPresent(headerMap, effectiveMap, List.of("startdate","start date","start"), "startdate");
            putIfPresent(headerMap, effectiveMap, List.of("deadline","enddate","end date","due date"), "deadline");
            putIfPresent(headerMap, effectiveMap, List.of("budget","projectbudget","project budget","amount"), "budget");
            putIfPresent(headerMap, effectiveMap, List.of("assignedemployeeids","assigned employee ids","assigned","assignedemployeeids","assigned_ids"), "assignedemployeeids");

            // Process rows using only effectiveMap (ignore extra columns)
            for (CSVRecord record : parser) {
                int rowNum = (int) record.getRecordNumber() + 1;
                try {
                    ProjectImport imp = mapRecordToProjectImport(record, effectiveMap);

                    // Validate required fields per-row; missing -> SKIPPED
                    String shortCode = safeTrim(imp.getShortCode());
                    String name = safeTrim(imp.getName());
                    String clientId = safeTrim(imp.getClientId());

                    if (shortCode == null || shortCode.isBlank()) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing shortCode", null));
                        continue;
                    }
                    if (name == null || name.isBlank()) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing name", null));
                        continue;
                    }
                    if (clientId == null || clientId.isBlank()) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing clientId", null));
                        continue;
                    }

                    // Duplicate check by shortCode
                    if (projectRepository.existsByShortCode(shortCode)) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Duplicate shortCode: " + shortCode, null));
                        continue;
                    }

                    // Parse optional dates
                    LocalDate startDate = null;
                    LocalDate deadline = null;
                    if (imp.getStartDate() != null && !imp.getStartDate().isBlank()) {
                        startDate = parseFlexibleDate(imp.getStartDate().trim());
                        if (startDate == null) {
                            results.add(new ImportResult(rowNum, "ERROR", "Invalid startDate: " + imp.getStartDate(), null));
                            continue;
                        }
                    }
                    if (imp.getDeadline() != null && !imp.getDeadline().isBlank()) {
                        deadline = parseFlexibleDate(imp.getDeadline().trim());
                        if (deadline == null) {
                            results.add(new ImportResult(rowNum, "ERROR", "Invalid deadline: " + imp.getDeadline(), null));
                            continue;
                        }
                    }

                    // Parse optional budget
                    BigDecimal budget = null;
                    if (imp.getBudget() != null && !imp.getBudget().isBlank()) {
                        try {
                            String num = imp.getBudget().replaceAll("[^0-9.\\-]", "");
                            if (!num.isBlank()) budget = new BigDecimal(num);
                        } catch (Exception e) {
                            results.add(new ImportResult(rowNum, "ERROR", "Invalid budget: " + imp.getBudget(), null));
                            continue;
                        }
                    }

                    // Parse optional assignedEmployeeIds
                    Set<String> assigned = new HashSet<>();
                    if (imp.getAssignedEmployeeIds() != null && !imp.getAssignedEmployeeIds().isBlank()) {
                        String cleaned = imp.getAssignedEmployeeIds().replaceAll("[\\[\\]\"]", "");
                        String[] parts = cleaned.split(",");
                        for (String p : parts) {
                            String t = p.trim();
                            if (!t.isEmpty()) assigned.add(t);
                        }
                    }

                    // Build and save Project (direct save; no notifications)
                    Project p = Project.builder()
                            .shortCode(shortCode)
                            .name(name)
                            .startDate(startDate)
                            .deadline(deadline)
                            .clientId(clientId)
                            .budget(budget)
                            .assignedEmployeeIds(assigned)
                            .build();

                    try { p.setCreatedBy(actorId); } catch (Exception ignored) {}
                    try { p.setAddedBy(actorId); } catch (Exception ignored) {}

                    Project saved = projectRepository.save(p);
                    results.add(new ImportResult(rowNum, "CREATED", null, saved.getId()));

                } catch (Exception exRow) {
                    log.error("Error importing row {}: {}", record.getRecordNumber(), exRow.getMessage(), exRow);
                    results.add(new ImportResult(rowNum, "ERROR", "Unhandled: " + safeMessage(exRow), null));
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse CSV file for import: {}", e.getMessage(), e);
            results.add(new ImportResult(0, "ERROR", "Failed to parse file: " + safeMessage(e), null));
        }

        return results;
    }

    // ----------------- helpers -----------------
    private static String normalizeHeaderKey(String h) {
        if (h == null) return "";
        return h.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private ProjectImport mapRecordToProjectImport(CSVRecord rec, Map<String, String> effectiveMap) {
        ProjectImport imp = new ProjectImport();

        java.util.function.Function<String, String> getVal = (key) -> {
            String actual = effectiveMap.get(key);
            if (actual != null && rec.isMapped(actual)) {
                String v = rec.get(actual);
                if (v != null) return v.trim();
            }
            return null;
        };

        // Required
        imp.setShortCode(getVal.apply("shortcode"));
        imp.setName(getVal.apply("name"));
        imp.setClientId(getVal.apply("clientid"));

        // Optional
        imp.setStartDate(getVal.apply("startdate"));
        imp.setDeadline(getVal.apply("deadline"));
        imp.setBudget(getVal.apply("budget"));
        imp.setAssignedEmployeeIds(getVal.apply("assignedemployeeids"));

        return imp;
    }

    private static String safeMessage(Exception e) {
        if (e == null) return "";
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    // Flexible date parser supporting multiple formats
    private LocalDate parseFlexibleDate(String input) {
        if (input == null || input.isBlank()) return null;

        List<String> patterns = List.of(
                "yyyy-MM-dd",
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "MM/dd/yyyy"
        );

        for (String pattern : patterns) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(input, fmt);
            } catch (Exception ignored) {}
        }
        return null;
    }

    // find first actual header present for any of the synonyms
    private Optional<String> findFirstHeaderForSynonyms(Map<String, String> headerMap, List<String> synonyms) {
        for (String s : synonyms) {
            String nk = normalizeHeaderKey(s);
            if (headerMap.containsKey(nk)) {
                return Optional.of(headerMap.get(nk));
            }
        }
        return Optional.empty();
    }

    // put first present header from synonyms into effectiveMap with targetKey
    private void putIfPresent(Map<String, String> headerMap, Map<String, String> effectiveMap, List<String> synonyms, String targetKey) {
        Optional<String> h = findFirstHeaderForSynonyms(headerMap, synonyms);
        h.ifPresent(actualHeader -> effectiveMap.put(targetKey, headerMap.get(normalizeHeaderKey(actualHeader))));
    }
}
