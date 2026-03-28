package com.erp.lead_service.service;

import com.erp.lead_service.dto.lead.LeadNoteRequestDto;
import com.erp.lead_service.dto.lead.LeadNoteDto;
import com.erp.lead_service.entity.Lead;
import com.erp.lead_service.entity.LeadNote;
import com.erp.lead_service.entity.NoteType;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.repository.LeadNoteRepository;
import com.erp.lead_service.repository.LeadRepository;
import com.erp.lead_service.service.LeadNoteService;
import com.erp.lead_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeadNoteServiceImpl implements LeadNoteService {

    private final LeadNoteRepository leadNoteRepository;
    private final LeadRepository leadRepository;
    private final JwtUtil jwtUtil;

    @Override
    public LeadNoteDto addNoteToLead(Long leadId, LeadNoteRequestDto dto, String authHeader) {
        String token = extractToken(authHeader);

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        // allow admin OR owner/addedBy to add

        String currentEmployeeId = jwtUtil.extractSubject(token);

        LeadNote note = LeadNote.builder()
                .lead(lead)
                .noteTitle(dto.getNoteTitle())
                .noteType(dto.getNoteType() != null ? NoteType.valueOf(dto.getNoteType().toUpperCase()) : null)
                .noteDetails(dto.getNoteDetails())
                .createdBy(currentEmployeeId)
                .createdAt(LocalDateTime.now())
                .build();

        LeadNote savedNote = leadNoteRepository.save(note);
        return mapToDto(savedNote);
    }

    @Override
    public List<LeadNoteDto> getNotesByLead(Long leadId, String authHeader) {
        String token = extractToken(authHeader);

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        // admin OR owner/addedBy can get notes

        List<LeadNote> notes = leadNoteRepository.findByLeadIdOrderByCreatedAtDesc(leadId);
        return notes.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteNote(Long leadId, Long noteId, String authHeader) {
        String token = extractToken(authHeader);

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        // admin OR owner/addedBy can delete

        LeadNote note = leadNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        // Optional: ensure note belongs to the lead
        if (!note.getLead().getId().equals(leadId)) {
            throw new ResourceNotFoundException("Note does not belong to the given lead");
        }

        leadNoteRepository.delete(note);
    }

    @Override
    public LeadNoteDto updateNote(Long leadId, Long noteId, LeadNoteRequestDto dto, String authHeader) {
        String token = extractToken(authHeader);

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        // admin OR owner/addedBy can update

        LeadNote note = leadNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        if (!note.getLead().getId().equals(leadId)) {
            throw new ResourceNotFoundException("Note does not belong to the given lead");
        }

        if (dto.getNoteTitle() != null) note.setNoteTitle(dto.getNoteTitle());
        if (dto.getNoteType() != null) note.setNoteType(NoteType.valueOf(dto.getNoteType().toUpperCase()));
        if (dto.getNoteDetails() != null) note.setNoteDetails(dto.getNoteDetails());

        LeadNote updatedNote = leadNoteRepository.save(note);
        return mapToDto(updatedNote);
    }

    private LeadNoteDto mapToDto(LeadNote note) {
        LeadNoteDto dto = new LeadNoteDto();
        dto.setId(note.getId());
        dto.setNoteTitle(note.getNoteTitle());
        dto.setNoteType(note.getNoteType() != null ? note.getNoteType().name() : null);
        dto.setNoteDetails(note.getNoteDetails());
        dto.setCreatedBy(note.getCreatedBy());
        dto.setCreatedAt(note.getCreatedAt());
        return dto;
    }

    private void checkLeadAccess(Lead lead, String token) {
        String currentEmployeeId = jwtUtil.extractSubject(token);
        boolean isAdmin = jwtUtil.isAdmin(token);

        if (!isAdmin && !lead.getLeadOwner().equals(currentEmployeeId) &&
                !lead.getAddedBy().equals(currentEmployeeId)) {
            throw new UnauthorizedAccessException("You don't have permission to access this lead's notes");
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedAccessException("Invalid Authorization header");
        }
        return authHeader.substring(7);
    }
}
