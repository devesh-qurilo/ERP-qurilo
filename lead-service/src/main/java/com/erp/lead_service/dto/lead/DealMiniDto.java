package com.erp.lead_service.dto.lead;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DealMiniDto {
    private String title;           // Deal Name
    private String pipeline;
    private String dealStage;
    private String dealCategory;
    private Double value;
    private LocalDate expectedCloseDate;
    private String dealAgent;       // employeeId
    private java.util.List<String> dealWatchers; // list of employeeIds
}
