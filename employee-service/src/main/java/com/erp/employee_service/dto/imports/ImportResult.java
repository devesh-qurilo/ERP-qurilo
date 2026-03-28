package com.erp.employee_service.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * rowNumber: human readable row number in CSV (1-based for data rows)
 * status: CREATED | SKIPPED | ERROR
 * reason: explanation when SKIPPED/ERROR
 * createdId: attendance id when CREATED
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    private int rowNumber;
    private String status;
    private String reason;
    private Long createdId;
}
