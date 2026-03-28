package com.erp.lead_service.dto.lead;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeadNoteDto {
    private Long id;
    private String noteTitle;
    private String noteType;
    private String noteDetails;
    private String createdBy;
    private LocalDateTime createdAt;
}
