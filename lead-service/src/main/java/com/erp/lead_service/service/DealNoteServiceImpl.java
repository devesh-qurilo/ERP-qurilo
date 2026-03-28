package com.erp.lead_service.service;

import com.erp.lead_service.dto.DealNoteDto;
import com.erp.lead_service.dto.lead.LeadNoteRequestDto;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.DealNote;
import com.erp.lead_service.entity.NoteType;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.repository.DealNoteRepository;
import com.erp.lead_service.repository.DealRepository;
import com.erp.lead_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DealNoteServiceImpl implements DealNoteService {

    private final DealNoteRepository dealNoteRepository;
    private final DealRepository dealRepository;
    private final JwtUtil jwtUtil;

    // ----------------- Create -----------------
    @Override
    @Transactional
    public DealNoteDto addNoteToDeal(Long dealId, LeadNoteRequestDto dto, String authHeader) {
        String token = extractToken(authHeader);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can add notes to deals");
        }

        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found"));

        String currentEmployeeId = jwtUtil.extractSubject(token);

        NoteType noteType;
        try {
            noteType = NoteType.valueOf(dto.getNoteType().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid noteType: " + dto.getNoteType());
        }

        DealNote note = DealNote.builder()
                .deal(deal)
                .noteTitle(dto.getNoteTitle())
                .noteType(noteType)
                .noteDetails(dto.getNoteDetails())
                .createdBy(currentEmployeeId)
                .createdAt(LocalDateTime.now())
                .build();

        DealNote savedNote = dealNoteRepository.save(note);
        return mapToDto(savedNote);
    }

    // ----------------- Read -----------------
    @Override
    public List<DealNoteDto> getNotesByDeal(Long dealId, String authHeader) {
        String token = extractToken(authHeader);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can add notes to deals");
        }

        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found"));

        checkDealAccess(deal, authHeader);

        return dealNoteRepository.findByDealIdOrderByCreatedAtDesc(dealId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ----------------- Delete -----------------
    @Override
    @Transactional
    public void deleteNote(Long dealId, Long noteId, String authHeader) {
        String token = extractToken(authHeader);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can delete notes");
        }

        DealNote note = dealNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        if (!note.getDeal().getId().equals(dealId)) {
            throw new ResourceNotFoundException("Note not found for this deal");
        }

        dealNoteRepository.delete(note);
    }

    // ----------------- Update -----------------
    @Override
    @Transactional
    public DealNoteDto updateNote(Long dealId, Long noteId, LeadNoteRequestDto dto, String authHeader) {
        String token = extractToken(authHeader);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can update notes");
        }

        DealNote note = dealNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        if (!note.getDeal().getId().equals(dealId)) {
            throw new ResourceNotFoundException("Note not found for this deal");
        }

        if (dto.getNoteTitle() != null) note.setNoteTitle(dto.getNoteTitle());
        if (dto.getNoteType() != null) {
            try {
                note.setNoteType(NoteType.valueOf(dto.getNoteType().toUpperCase()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid noteType: " + dto.getNoteType());
            }
        }
        if (dto.getNoteDetails() != null) note.setNoteDetails(dto.getNoteDetails());

        DealNote updatedNote = dealNoteRepository.save(note);
        return mapToDto(updatedNote);
    }

    // ----------------- Helpers -----------------
    private DealNoteDto mapToDto(DealNote note) {
        DealNoteDto dto = new DealNoteDto();
        dto.setId(note.getId());
        dto.setNoteTitle(note.getNoteTitle());
        dto.setNoteType(note.getNoteType().name());
        dto.setNoteDetails(note.getNoteDetails());
        dto.setCreatedBy(note.getCreatedBy());
        dto.setCreatedAt(note.getCreatedAt());
        return dto;
    }

    /**
     * Access rules:
     * - Admin: always allowed
     * - Non-admin: allowed if user is the dealAgent, one of the watchers, or the lead's owner/creator.
     */
    private void checkDealAccess(Deal deal, String authHeader) {
        String token = extractToken(authHeader);
        String currentEmployeeId = jwtUtil.extractSubject(token);
        boolean isAdmin = jwtUtil.isAdmin(token);

        if (isAdmin) return;

        boolean isAgent = currentEmployeeId != null && currentEmployeeId.equals(deal.getDealAgent());
        boolean isWatcher = deal.getDealWatchers() != null && deal.getDealWatchers().contains(currentEmployeeId);
        boolean isLeadOwnerOrCreator = deal.getLead() != null &&
                (currentEmployeeId != null) &&
                (currentEmployeeId.equals(deal.getLead().getLeadOwner()) ||
                        currentEmployeeId.equals(deal.getLead().getAddedBy()));

        if (!(isAgent || isWatcher || isLeadOwnerOrCreator)) {
            throw new UnauthorizedAccessException("You don't have permission to access this deal's notes");
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null) return null;
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }
}
