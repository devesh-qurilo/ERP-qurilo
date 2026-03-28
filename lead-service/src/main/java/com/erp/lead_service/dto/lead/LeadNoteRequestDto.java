package com.erp.lead_service.dto.lead;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeadNoteRequestDto {
    @NotBlank
    private String noteTitle;
    @NotBlank private String noteType; // PUBLIC, PRIVATE
    @NotBlank private String noteDetails;
}
