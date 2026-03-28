package com.erp.lead_service.dto.Import;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    private int rowNumber;
    private String status; // CREATED, SKIPPED, ERROR
    private String reason; // optional (e.g., "Duplicate email", "Missing name and contact", "Parse error")
    private Long createdLeadId; // optional
}
