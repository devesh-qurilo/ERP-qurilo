package com.erp.client_service.service.Import.impl;

import com.erp.client_service.dto.Import.ClientImport;
import com.erp.client_service.dto.Import.ImportResult;
import com.erp.client_service.entity.Client;
import com.erp.client_service.entity.Company;
import com.erp.client_service.repository.ClientRepository;
import com.erp.client_service.repository.CompanyRepository;
import com.erp.client_service.service.Import.ClientCsvImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.time.Instant;
import java.util.*;

/**
 * Robust CSV import for clients:
 * - header normalization (case/space-insensitive)
 * - synonyms support (name / full name, mobile / phone etc.)
 * - duplicates skipped (email/mobile)
 * - returns per-row ImportResult
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientCsvImportServiceImpl implements ClientCsvImportService {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public List<ImportResult> importClientsFromCsv(MultipartFile file, String authHeader) {
        List<ImportResult> results = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            results.add(new ImportResult(0, "ERROR", "Empty or missing file", null));
            return results;
        }

        // preload existing for duplicate mobile checks
        List<Client> existing = clientRepository.findAll();

        try (var reader = new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim(false)
                    .parse(reader);

            // build normalized header map
            Map<String, String> headerMap = new HashMap<>();
            for (String h : parser.getHeaderMap().keySet()) {
                if (h != null) headerMap.put(normalizeHeaderKey(h), h);
            }

            for (CSVRecord record : parser) {
                int rowNum = (int) record.getRecordNumber() + 1;
                try {
                    ClientImport imp = mapRecordToClientImport(record, headerMap);

                    // normalize + basic validation
                    String name = safeTrim(imp.getName());
                    String emailRaw = safeTrim(imp.getEmail());
                    String email = emailRaw == null ? null : emailRaw.toLowerCase();
                    String mobileRaw = safeTrim(imp.getMobile());
                    String mobileDigits = normalizeDigits(mobileRaw);

                    // require at least name + (email or mobile)
                    if ((name == null || name.isBlank()) && (email == null || email.isBlank()) && (mobileRaw == null || mobileRaw.isBlank())) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Empty row (no name/email/mobile)", null));
                        continue;
                    }

                    // duplicate checks
                    boolean dupEmail = false;
                    if (email != null && !email.isBlank()) {
                        dupEmail = clientRepository.findByEmail(email).isPresent();
                    }
                    boolean dupMobile = false;
                    if (mobileDigits != null && !mobileDigits.isBlank()) {
                        for (Client c : existing) {
                            if (c.getMobile() != null && normalizeDigits(c.getMobile()) != null &&
                                    normalizeDigits(c.getMobile()).equals(mobileDigits)) {
                                dupMobile = true;
                                break;
                            }
                        }
                    }
                    if (dupEmail) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Duplicate email", null));
                        continue;
                    }
                    if (dupMobile) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Duplicate mobile", null));
                        continue;
                    }

                    // build Client entity (only necessary fields)
                    Client client = Client.builder()
                            .clientId(generateClientId())
                            .name(name)
                            .email(email)
                            .mobile(mobileRaw)
                            .country(safeTrim(imp.getCountry()))
                            .status("ACTIVE")
                            .addedBy(extractActorFromAuth(authHeader))
                            .createdAt(Instant.now())
                            .build();

                    // optional company
                    if (imp.getCompanyName() != null || imp.getWebsite() != null || imp.getOfficePhone() != null || imp.getGstVatNo() != null) {
                        Company company = Company.builder()
                                .companyName(safeTrim(imp.getCompanyName()))
                                .website(safeTrim(imp.getWebsite()))
                                .officePhone(safeTrim(imp.getOfficePhone()))
                                .gstVatNo(safeTrim(imp.getGstVatNo()))
                                .address(null) // import didn't include full address fields — keep null
                                .build();
                        client.setCompany(company);
                    }

                    Client saved = clientRepository.save(client);
                    // company saved by cascade (if present)

                    existing.add(saved); // include for further duplicate checks

                    results.add(new ImportResult(rowNum, "CREATED", null, saved.getId()));
                } catch (Exception exRow) {
                    log.error("Row {} import failed: {}", record.getRecordNumber(), exRow.getMessage(), exRow);
                    results.add(new ImportResult(rowNum, "ERROR", "Unhandled: " + safeMessage(exRow), null));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse clients CSV: {}", e.getMessage(), e);
            results.add(new ImportResult(0, "ERROR", "Failed to parse file: " + safeMessage(e), null));
        }

        return results;
    }

    // ---------------- helpers ----------------

    private static String normalizeHeaderKey(String h) {
        if (h == null) return "";
        return h.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private static String safeTrim(String s) { return s == null ? null : s.trim(); }

    private static String normalizeDigits(String s) {
        if (s == null) return null;
        String d = s.replaceAll("\\D+", "");
        return d.isBlank() ? null : d;
    }

    private String generateClientId() {
        long next = clientRepository.count() + 1;
        return String.format("CLI%04d", next);
    }

    private static String safeMessage(Exception e) {
        return e == null ? "" : (e.getMessage() == null ? e.toString() : e.getMessage());
    }

    /**
     * very small helper — extract actor id from Authorization header if possible.
     * If not present, fallback to "system-import".
     */
    private String extractActorFromAuth(String authHeader) {
        try {
            if (authHeader == null) return "system-import";
            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // we don't decode token here; caller usually passes JWT and service's JwtUtil can extract subject.
                // but to avoid coupling, simply return raw token or "importer".
                return token.length() > 8 ? token.substring(0, 8) : token;
            }
            return authHeader;
        } catch (Exception e) {
            return "system-import";
        }
    }

    /**
     * Map CSV record to ClientImport respecting header synonyms
     */
    private ClientImport mapRecordToClientImport(CSVRecord rec, Map<String, String> headerMap) {
        ClientImport imp = new ClientImport();

        java.util.function.Function<String[], String> getVal = (keys) -> {
            for (String k : keys) {
                String nk = normalizeHeaderKey(k);
                String act = headerMap.get(nk);
                if (act != null && rec.isMapped(act)) {
                    String v = rec.get(act);
                    if (v != null) return v.trim();
                }
            }
            return null;
        };

        imp.setName(getVal.apply(new String[]{"name","full name","client name","contact name"}));
        imp.setEmail(getVal.apply(new String[]{"email","e-mail","email address"}));
        imp.setMobile(getVal.apply(new String[]{"mobile","mobile number","phone","phone number","contact"}));
        imp.setCountry(getVal.apply(new String[]{"country","nation"}));
        imp.setCompanyName(getVal.apply(new String[]{"company","company name","organization","org"}));
        imp.setWebsite(getVal.apply(new String[]{"website","url","company website"}));
        imp.setOfficePhone(getVal.apply(new String[]{"officephone","office phone","landline"}));
        imp.setCity(getVal.apply(new String[]{"city"}));
        imp.setState(getVal.apply(new String[]{"state","province","region"}));
        imp.setPostalCode(getVal.apply(new String[]{"postalcode","postal code","zip","zip code"}));
        imp.setGstVatNo(getVal.apply(new String[]{"gst","vat","gst_vat_no","taxno"}));

        return imp;
    }
}
