package com.erp.lead_service.dto.Import;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealImport {
    private String title;
    private String value; // parse to Double in service
    private String dealStage;
    private String leadName; // note: using leadName instead of leadId
    private String expectedCloseDate; // parse to LocalDate (yyyy-MM-dd preferred)
    private String pipeline;
}
