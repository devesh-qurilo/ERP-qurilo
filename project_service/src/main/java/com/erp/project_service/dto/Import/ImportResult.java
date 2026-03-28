package com.erp.project_service.dto.Import;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    private int rowNumber;
    private String status;   // CREATED / SKIPPED / ERROR
    private String reason;   // explanation
    private Long createdId;  // if created
}
