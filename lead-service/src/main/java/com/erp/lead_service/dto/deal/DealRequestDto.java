package com.erp.lead_service.dto.deal;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DealRequestDto {
    private String title;
    private Double value;
    private String currency;
    private String dealStage;
    private String dealAgent;
    private List<String> dealWatchers;
    private Long leadId;
    private LocalDate expectedCloseDate;
    private String pipeline;
    private String dealCategory;
    private String dealContact; // optional - lead contact
}
