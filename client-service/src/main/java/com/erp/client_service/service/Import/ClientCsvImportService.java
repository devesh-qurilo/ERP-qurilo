package com.erp.client_service.service.Import;

import com.erp.client_service.dto.Import.ImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ClientCsvImportService {
    List<ImportResult> importClientsFromCsv(MultipartFile file, String authHeader);
}
