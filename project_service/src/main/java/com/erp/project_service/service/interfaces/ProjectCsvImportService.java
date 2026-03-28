package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.Import.ImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectCsvImportService {
    List<ImportResult> importProjectsFromCsv(MultipartFile file, String actorId);
}
