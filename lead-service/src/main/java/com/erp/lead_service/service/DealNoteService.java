package com.erp.lead_service.service;

import com.erp.lead_service.dto.DealNoteDto;
import com.erp.lead_service.dto.lead.LeadNoteRequestDto;

import java.util.List;

public interface DealNoteService {

    DealNoteDto addNoteToDeal(Long dealId, LeadNoteRequestDto dto, String authHeader);

    List<DealNoteDto> getNotesByDeal(Long dealId, String authHeader);

    void deleteNote(Long dealId, Long noteId, String authHeader);

    DealNoteDto updateNote(Long dealId, Long noteId, LeadNoteRequestDto dto, String authHeader);
}
