//package com.erp.lead_service.service;
//
//import com.erp.lead_service.dto.Import.DealImport;
//import com.erp.lead_service.dto.Import.ImportResult;
//import com.erp.lead_service.entity.Deal;
//import com.erp.lead_service.entity.Lead;
//import com.erp.lead_service.repository.DealRepository;
//import com.erp.lead_service.repository.LeadRepository;
//import com.erp.lead_service.service.DealCsvImportService;
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
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class DealCsvImportServiceImpl implements DealCsvImportService {
//
//    private final DealRepository dealRepository;
//    private final LeadRepository leadRepository;
//
//    @Override
//    @Transactional
//    public List<ImportResult> importDealsFromCsv(MultipartFile file, String authHeader) {
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
//            // Normalize header map
//            Map<String, String> headerMap = new HashMap<>();
//            for (String h : parser.getHeaderMap().keySet()) {
//                if (h != null) headerMap.put(normalizeHeaderKey(h), h);
//            }
//
//            for (CSVRecord record : parser) {
//                int rowNum = (int) record.getRecordNumber() + 1;
//                try {
//                    DealImport imp = mapRecordToDealImport(record, headerMap);
//
//                    // Title mandatory
//                    String title = safeTrim(imp.getTitle());
//                    if (title == null || title.isBlank()) {
//                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing title", null));
//                        continue;
//                    }
//
//                    // Lead name mandatory
//                    String leadName = safeTrim(imp.getLeadName());
//                    if (leadName == null || leadName.isBlank()) {
//                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing lead name", null));
//                        continue;
//                    }
//
//                    // Find lead by name (case-insensitive)
//                    Optional<Lead> leadOpt = leadRepository.findByNameIgnoreCase(leadName);
//                    if (leadOpt.isEmpty()) {
//                        results.add(new ImportResult(rowNum, "SKIPPED", "Lead not found: " + leadName, null));
//                        continue;
//                    }
//                    Lead lead = leadOpt.get();
//
//                    // Parse value
//                    Double value = null;
//                    if (imp.getValue() != null && !imp.getValue().isBlank()) {
//                        try {
//                            String v = imp.getValue().replaceAll("[^0-9.\\-]", "");
//                            if (!v.isBlank()) value = Double.parseDouble(v);
//                        } catch (NumberFormatException nfe) {
//                            results.add(new ImportResult(rowNum, "ERROR", "Invalid numeric value: " + imp.getValue(), null));
//                            continue;
//                        }
//                    }
//
//                    // Parse expectedCloseDate (with flexible formats)
//                    LocalDate expected = null;
//                    if (imp.getExpectedCloseDate() != null && !imp.getExpectedCloseDate().isBlank()) {
//                        String d = imp.getExpectedCloseDate().trim();
//                        expected = parseFlexibleDate(d);
//                        if (expected == null) {
//                            results.add(new ImportResult(rowNum, "ERROR", "Invalid date format: " + d, null));
//                            continue;
//                        }
//                    }
//
//                    // Duplicate check (title + lead)
//                    boolean dup = false;
//                    try {
//                        dup = dealRepository.existsByTitleIgnoreCaseAndLeadId(title, lead.getId());
//                    } catch (Exception e) {
//                        log.warn("Duplicate check query failed for row {}: {}", rowNum, e.getMessage());
//                    }
//                    if (dup) {
//                        results.add(new ImportResult(rowNum, "SKIPPED", "Duplicate deal (title + lead)", null));
//                        continue;
//                    }
//
//                    // Create and save deal
//                    Deal entity = new Deal();
//                    entity.setTitle(title);
//                    entity.setValue(value);
//                    entity.setDealStage(safeTrim(imp.getDealStage()));
//                    entity.setPipeline(safeTrim(imp.getPipeline()));
//                    entity.setExpectedCloseDate(expected);
//                    entity.setLead(lead);
//                    try { entity.setCreatedAt(LocalDateTime.now()); } catch (Exception ignored) {}
//
//                    Deal saved = dealRepository.save(entity);
//                    results.add(new ImportResult(rowNum, "CREATED", null, saved.getId()));
//
//                } catch (Exception exRow) {
//                    log.error("Error importing row {}: {}", record.getRecordNumber(), exRow.getMessage(), exRow);
//                    results.add(new ImportResult(rowNum, "ERROR", "Unhandled: " + safeMessage(exRow), null));
//                }
//            }
//
//        } catch (Exception ex) {
//            log.error("Failed to parse CSV file for import: {}", ex.getMessage(), ex);
//            results.add(new ImportResult(0, "ERROR", "Failed to parse file: " + safeMessage(ex), null));
//        }
//
//        return results;
//    }
//
//    // ---------- helper methods ----------
//
//    private static String normalizeHeaderKey(String h) {
//        if (h == null) return "";
//        return h.trim().toLowerCase().replaceAll("\\s+", "");
//    }
//
//    private static String safeTrim(String s) {
//        return s == null ? null : s.trim();
//    }
//
//    private DealImport mapRecordToDealImport(CSVRecord rec, Map<String, String> headerMap) {
//        DealImport imp = new DealImport();
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
//        imp.setTitle(getVal.apply(new String[]{"title","deal name","name"}));
//        imp.setValue(getVal.apply(new String[]{"value","deal value","amount"}));
//        imp.setDealStage(getVal.apply(new String[]{"dealstage","stage"}));
//        imp.setLeadName(getVal.apply(new String[]{"leadname","lead name","lead","lead_name"}));
//        imp.setExpectedCloseDate(getVal.apply(new String[]{"expectedclosedate","expected close date","close date","expected_close_date"}));
//        imp.setPipeline(getVal.apply(new String[]{"pipeline"}));
//
//        return imp;
//    }
//
//    private static String safeMessage(Exception e) {
//        if (e == null) return "";
//        return e.getMessage() == null ? e.toString() : e.getMessage();
//    }
//
//    /**
//     * Flexible date parser supporting multiple formats:
//     * yyyy-MM-dd, dd-MM-yyyy, dd/MM/yyyy, MM/dd/yyyy
//     */
//    private LocalDate parseFlexibleDate(String input) {
//        if (input == null || input.isBlank()) return null;
//
//        List<String> patterns = List.of(
//                "yyyy-MM-dd",  // ISO
//                "dd-MM-yyyy",  // Indian
//                "dd/MM/yyyy",  // common
//                "MM/dd/yyyy"   // US fallback
//        );
//
//        for (String pattern : patterns) {
//            try {
//                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
//                LocalDate parsed = LocalDate.parse(input, fmt);
//                log.debug("Parsed date '{}' using pattern '{}'", input, pattern);
//                return parsed;
//            } catch (Exception ignored) {}
//        }
//        return null; // no valid format
//    }
//}

package com.erp.lead_service.service;

import com.erp.lead_service.dto.Import.DealImport;
import com.erp.lead_service.dto.Import.ImportResult;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.Lead;
import com.erp.lead_service.repository.DealRepository;
import com.erp.lead_service.repository.LeadRepository;
import com.erp.lead_service.service.DealCsvImportService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DealCsvImportServiceImpl implements DealCsvImportService {

    private final DealRepository dealRepository;
    private final LeadRepository leadRepository;

    @Override
    @Transactional
    public List<ImportResult> importDealsFromCsv(MultipartFile file, String authHeader) {
        List<ImportResult> results = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            results.add(new ImportResult(0, "ERROR", "Empty or missing file", null));
            return results;
        }

        try (InputStream in = file.getInputStream();
             InputStreamReader reader = new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {

            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim(false)
                    .withAllowMissingColumnNames() // allow blank header names (robust)
                    .parse(reader);

            // Normalize header map robustly: ignore blank normalized keys, warn on duplicates
            Map<String, String> headerMap = new HashMap<>();
            for (String raw : parser.getHeaderMap().keySet()) {
                if (raw == null) continue;
                String nk = normalizeHeaderKey(raw);
                if (nk.isBlank()) {
                    log.debug("Ignoring blank CSV header column (raw='{}')", raw);
                    continue;
                }
                if (headerMap.containsKey(nk)) {
                    log.warn("Duplicate normalized CSV header '{}' found (keeping first: '{}' ; ignoring: '{}')",
                            nk, headerMap.get(nk), raw);
                    continue;
                }
                headerMap.put(nk, raw);
            }

            // ensure required headers exist (title + leadName) using synonyms used earlier
            List<String> titleSyn = List.of("title","deal name","name");
            List<String> leadSyn = List.of("leadname","lead name","lead","lead_name");

            Optional<String> titleHeader = findFirstHeaderForSynonyms(headerMap, titleSyn);
            Optional<String> leadHeader = findFirstHeaderForSynonyms(headerMap, leadSyn);

            if (titleHeader.isEmpty() || leadHeader.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                if (titleHeader.isEmpty()) sb.append("Missing required header: title. ");
                if (leadHeader.isEmpty()) sb.append("Missing required header: leadName. ");
                results.add(new ImportResult(0, "ERROR", sb.toString().trim(), null));
                return results;
            }

            // Build effective map (only supported keys) - keep your original mapping names
            Map<String, String> effectiveMap = new HashMap<>();
            effectiveMap.put("title", headerMap.get(normalizeHeaderKey(titleHeader.get())));
            effectiveMap.put("leadname", headerMap.get(normalizeHeaderKey(leadHeader.get())));
            putIfPresent(headerMap, effectiveMap, new String[]{"value","deal value","amount"}, "value");
            putIfPresent(headerMap, effectiveMap, new String[]{"dealstage","stage"}, "dealstage");
            putIfPresent(headerMap, effectiveMap, new String[]{"expectedclosedate","expected close date","close date","expected_close_date"}, "expectedclosedate");
            putIfPresent(headerMap, effectiveMap, new String[]{"pipeline"}, "pipeline");

            for (CSVRecord record : parser) {
                int rowNum = (int) record.getRecordNumber() + 1;
                try {
                    DealImport imp = mapRecordToDealImport(record, effectiveMap);

                    // Title mandatory
                    String title = safeTrim(imp.getTitle());
                    if (title == null || title.isBlank()) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing title", null));
                        continue;
                    }

                    // Lead name mandatory
                    String leadName = safeTrim(imp.getLeadName());
                    if (leadName == null || leadName.isBlank()) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing lead name", null));
                        continue;
                    }

                    // Find lead by name (case-insensitive)
                    Optional<Lead> leadOpt = leadRepository.findByNameIgnoreCase(leadName);
                    if (leadOpt.isEmpty()) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Lead not found: " + leadName, null));
                        continue;
                    }
                    Lead lead = leadOpt.get();

                    // Parse value
                    Double value = null;
                    if (imp.getValue() != null && !imp.getValue().isBlank()) {
                        try {
                            String v = imp.getValue().replaceAll("[^0-9.\\-]", "");
                            if (!v.isBlank()) value = Double.parseDouble(v);
                        } catch (NumberFormatException nfe) {
                            results.add(new ImportResult(rowNum, "ERROR", "Invalid numeric value: " + imp.getValue(), null));
                            continue;
                        }
                    }

                    // Parse expectedCloseDate (with flexible formats)
                    LocalDate expected = null;
                    if (imp.getExpectedCloseDate() != null && !imp.getExpectedCloseDate().isBlank()) {
                        String d = imp.getExpectedCloseDate().trim();
                        expected = parseFlexibleDate(d);
                        if (expected == null) {
                            results.add(new ImportResult(rowNum, "ERROR", "Invalid date format: " + d, null));
                            continue;
                        }
                    }

                    // Duplicate check (title + lead)
                    boolean dup = false;
                    try {
                        dup = dealRepository.existsByTitleIgnoreCaseAndLeadId(title, lead.getId());
                    } catch (Exception e) {
                        log.warn("Duplicate check query failed for row {}: {}", rowNum, e.getMessage());
                    }
                    if (dup) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Duplicate deal (title + lead)", null));
                        continue;
                    }

                    // Create and save deal
                    Deal entity = new Deal();
                    entity.setTitle(title);
                    entity.setValue(value);
                    entity.setDealStage(safeTrim(imp.getDealStage()));
                    entity.setPipeline(safeTrim(imp.getPipeline()));
                    entity.setExpectedCloseDate(expected);
                    entity.setLead(lead);
                    try { entity.setCreatedAt(LocalDateTime.now()); } catch (Exception ignored) {}

                    Deal saved = dealRepository.save(entity);
                    results.add(new ImportResult(rowNum, "CREATED", null, saved.getId()));

                } catch (Exception exRow) {
                    log.error("Error importing row {}: {}", record.getRecordNumber(), exRow.getMessage(), exRow);
                    results.add(new ImportResult(rowNum, "ERROR", "Unhandled: " + safeMessage(exRow), null));
                }
            }

        } catch (Exception ex) {
            log.error("Failed to parse CSV file for import: {}", ex.getMessage(), ex);
            results.add(new ImportResult(0, "ERROR", "Failed to parse file: " + safeMessage(ex), null));
        }

        return results;
    }

    // ---------- helper methods ----------

    private static String normalizeHeaderKey(String h) {
        if (h == null) return "";
        return h.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private DealImport mapRecordToDealImport(CSVRecord rec, Map<String, String> headerMap) {
        DealImport imp = new DealImport();

        java.util.function.Function<String[], String> getVal = (keys) -> {
            for (String k : keys) {
                String nk = normalizeHeaderKey(k);
                String actual = headerMap.get(nk);
                if (actual != null && rec.isMapped(actual)) {
                    String v = rec.get(actual);
                    if (v != null) return v.trim();
                }
            }
            return null;
        };

        imp.setTitle(getVal.apply(new String[]{"title","deal name","name"}));
        imp.setValue(getVal.apply(new String[]{"value","deal value","amount"}));
        imp.setDealStage(getVal.apply(new String[]{"dealstage","stage"}));
        imp.setLeadName(getVal.apply(new String[]{"leadname","lead name","lead","lead_name"}));
        imp.setExpectedCloseDate(getVal.apply(new String[]{"expectedclosedate","expected close date","close date","expected_close_date"}));
        imp.setPipeline(getVal.apply(new String[]{"pipeline"}));

        return imp;
    }

    private static String safeMessage(Exception e) {
        if (e == null) return "";
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    /**
     * Flexible date parser supporting multiple formats:
     * yyyy-MM-dd, dd-MM-yyyy, dd/MM/yyyy, MM/dd/yyyy
     */
    private LocalDate parseFlexibleDate(String input) {
        if (input == null || input.isBlank()) return null;

        List<String> patterns = List.of(
                "yyyy-MM-dd",  // ISO
                "dd-MM-yyyy",  // Indian
                "dd/MM/yyyy",  // common
                "MM/dd/yyyy"   // US fallback
        );

        for (String pattern : patterns) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
                LocalDate parsed = LocalDate.parse(input, fmt);
                log.debug("Parsed date '{}' using pattern '{}'", input, pattern);
                return parsed;
            } catch (Exception ignored) {}
        }
        return null; // no valid format
    }

    private Optional<String> findFirstHeaderForSynonyms(Map<String, String> headerMap, List<String> synonyms) {
        for (String s : synonyms) {
            String nk = normalizeHeaderKey(s);
            if (headerMap.containsKey(nk)) {
                return Optional.of(headerMap.get(nk));
            }
        }
        return Optional.empty();
    }

    private void putIfPresent(Map<String, String> headerMap, Map<String, String> effectiveMap, String[] synonyms, String targetKey) {
        for (String s : synonyms) {
            String nk = normalizeHeaderKey(s);
            if (headerMap.containsKey(nk)) {
                effectiveMap.put(targetKey, headerMap.get(nk));
                return;
            }
        }
    }
}

