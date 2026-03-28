package com.erp.lead_service.dto.deal;

import com.erp.lead_service.dto.CommentResponseDto;
import com.erp.lead_service.dto.EmployeeMetaDto;
import com.erp.lead_service.dto.PriorityDto;
import com.erp.lead_service.entity.DealDocument;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealResponseDto {
    private Long id;
    private String title;
    private Double value;
    private String currency;
    private String dealStage;

    private String dealAgent;
    private List<String> dealWatchers;
    private Long leadId;
    // NEW: show lead's name & mobile in deal response
    private String leadName;
    private String leadMobile;
    //New
    private String leadEmail;
    private String leadCompany;

    private LocalDate expectedCloseDate;
    private String pipeline;
    private String dealCategory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DealFollowupDto> followups = new ArrayList<>();
    // existing enrichments
    private List<String> tags;
    private List<CommentResponseDto> comments;
    private List<EmployeeMetaDto> assignedEmployeesMeta;

    // NEW: meta for agent & watchers (what you asked)
    private EmployeeMetaDto dealAgentMeta;
    private List<EmployeeMetaDto> dealWatchersMeta;

    // NEW: priority for this deal (will be filled by service enrichment)
    private PriorityDto priority;

}
