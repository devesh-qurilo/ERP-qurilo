package com.erp.client_service.dto.Import;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Import result per-row
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    private Integer rowNumber;   // 0 for file-level errors
    private String status;       // CREATED / SKIPPED / ERROR
    private String reason;       // message if any
    private Long createdId;      // DB id when created
}
