package com.erp.client_service.service.note.impl;

import com.erp.client_service.dto.note.NoteDto;
import com.erp.client_service.entity.ClientNote;
import com.erp.client_service.exception.ResourceNotFoundException;
import com.erp.client_service.repository.ClientNoteRepository;
import com.erp.client_service.service.note.NoteService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final ClientNoteRepository repo;

    @Override
    public NoteDto addNote(Long clientId, NoteDto dto, String createdBy) {
        ClientNote n = ClientNote.builder()
                .clientId(clientId)
                .title(dto.getTitle())
                .detail(dto.getDetail())
                .type(dto.getType() == null ? com.erp.client_service.entity.NoteType.PUBLIC : dto.getType())
                .createdBy(createdBy)
                .createdAt(Instant.now())
                .build();
        ClientNote saved = repo.save(n);
        return toDto(saved);
    }

    @Override
    @Transactional
    public List<NoteDto> listNotes(Long clientId) {
        return repo.findByClientId(clientId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void deleteNote(Long noteId) {
        if (!repo.existsById(noteId)) throw new ResourceNotFoundException("Note not found");
        repo.deleteById(noteId);
    }

    @Override
    public NoteDto updateNode(Long noteId, NoteDto dto) {
        ClientNote n = repo.findById(noteId).orElseThrow(() -> new ResourceNotFoundException("Note not found"));
        if (dto.getTitle() != null) {
            n.setTitle(dto.getTitle());
        }
        if (dto.getDetail() != null) {
            n.setDetail(dto.getDetail());
        }
        if (dto.getType() != null) {
            n.setType(dto.getType());
        }

        ClientNote saved = repo.save(n);
        NoteDto updated = toDto(saved);
        return updated;
    }

    private NoteDto toDto(ClientNote n) {
        return NoteDto.builder()
                .id(n.getId())
                .clientId(n.getClientId())
                .title(n.getTitle())
                .detail(n.getDetail())
                .type(n.getType())
                .createdBy(n.getCreatedBy())
                .createdAt(n.getCreatedAt() == null ? null : n.getCreatedAt().toString())
                .build();
    }
}
