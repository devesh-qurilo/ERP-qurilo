package com.erp.lead_service.controller;

import com.erp.lead_service.dto.DealNoteDto;
import com.erp.lead_service.dto.lead.LeadNoteRequestDto;
import com.erp.lead_service.service.DealNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deals/{dealId}/notes")
@RequiredArgsConstructor
public class DealNoteController {

    private final DealNoteService dealNoteService;

    // Create
    @PostMapping
    public ResponseEntity<DealNoteDto> addNote(@PathVariable Long dealId,
                                               @Valid @RequestBody LeadNoteRequestDto dto,
                                               @RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(dealNoteService.addNoteToDeal(dealId, dto, auth));
    }

    // Read (list)
    @GetMapping
    public ResponseEntity<List<DealNoteDto>> getNotes(@PathVariable Long dealId,
                                                      @RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(dealNoteService.getNotesByDeal(dealId, auth));
    }

    // Update
    @PutMapping("/{noteId}")
    public ResponseEntity<DealNoteDto> updateNote(@PathVariable Long dealId,
                                                  @PathVariable Long noteId,
                                                  @Valid @RequestBody LeadNoteRequestDto dto,
                                                  @RequestHeader("Authorization") String auth) {
        return ResponseEntity.ok(dealNoteService.updateNote(dealId, noteId, dto, auth));
    }

    // Delete
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long dealId,
                                           @PathVariable Long noteId,
                                           @RequestHeader("Authorization") String auth) {
        dealNoteService.deleteNote(dealId, noteId, auth);
        return ResponseEntity.noContent().build();
    }
}
