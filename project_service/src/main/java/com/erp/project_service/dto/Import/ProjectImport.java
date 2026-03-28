package com.erp.project_service.dto.Import;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectImport {
    private String shortCode;
    private String name;
    private String startDate;     // parsed later
    private String deadline;      // parsed later (optional)
    private String clientId;
    private String budget;        // parsed to BigDecimal
    private String assignedEmployeeIds; // comma-separated
}
