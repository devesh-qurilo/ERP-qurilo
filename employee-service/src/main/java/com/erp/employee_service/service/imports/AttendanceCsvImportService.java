package com.erp.employee_service.service.imports;

import com.erp.employee_service.dto.imports.ImportResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttendanceCsvImportService {
    /**
     * Import attendance rows from a CSV file.
     *
     * @param file      uploaded CSV file
     * @param markedBy  admin employeeId who marks (optional, can be null)
     * @param overwrite if true then existing attendance rows will be updated
     * @return list of ImportResult per row
     */
    List<ImportResult> importAttendanceFromCsv(MultipartFile file, String markedBy, boolean overwrite);
}
