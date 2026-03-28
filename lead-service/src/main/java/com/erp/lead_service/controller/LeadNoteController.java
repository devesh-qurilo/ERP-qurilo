package com.erp.lead_service.controller;

import com.erp.lead_service.dto.lead.LeadNoteRequestDto;
import com.erp.lead_service.dto.lead.LeadNoteDto;
import com.erp.lead_service.service.LeadNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leads/{leadId}/notes")
@RequiredArgsConstructor
public class LeadNoteController {

    private final LeadNoteService leadNoteService;

    @PostMapping
    public ResponseEntity<LeadNoteDto> addNote(
            @PathVariable Long leadId,
            @Valid @RequestBody LeadNoteRequestDto dto,
            @RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(leadNoteService.addNoteToLead(leadId, dto, auth));
    }

    @GetMapping
    public ResponseEntity<List<LeadNoteDto>> getNotes(
            @PathVariable Long leadId,
            @RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(leadNoteService.getNotesByLead(leadId, auth));
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<LeadNoteDto> updateNote(
            @PathVariable Long leadId,
            @PathVariable Long noteId,
            @Valid @RequestBody LeadNoteRequestDto dto,
            @RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(leadNoteService.updateNote(leadId,  noteId, dto, auth));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long leadId,
            @PathVariable Long noteId,
            @RequestHeader("Authorization") String auth) {
        leadNoteService.deleteNote(leadId, noteId, auth);
        return ResponseEntity.noContent().build();
    }
}