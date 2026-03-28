package com.erp.lead_service.service;

import org.springframework.web.multipart.MultipartFile;
import com.erp.lead_service.dto.Import.ImportResult;

import java.util.List;

public interface DealCsvImportService {
    /**
     * Import deals from a CSV file. CSV headers are case-insensitive and trimmed.
     * Expected headers (any case/spacing): title, value, dealStage, leadName, expectedCloseDate, pipeline
     *
     * Returns per-row ImportResult with CREATED / SKIPPED / ERROR.
     */
    List<ImportResult> importDealsFromCsv(MultipartFile file, String authHeader);
}
