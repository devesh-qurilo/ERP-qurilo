package com.erp.lead_service.service;

import com.erp.lead_service.dto.lead.LeadNoteRequestDto;
import com.erp.lead_service.dto.lead.LeadNoteDto;
import java.util.List;

public interface LeadNoteService {
    LeadNoteDto addNoteToLead(Long leadId, LeadNoteRequestDto dto, String authHeader);
    List<LeadNoteDto> getNotesByLead(Long leadId, String authHeader);
    void deleteNote(Long leadId, Long noteId, String authHeader);
    LeadNoteDto updateNote(Long leadId, Long noteId, LeadNoteRequestDto dto, String authHeader);
}