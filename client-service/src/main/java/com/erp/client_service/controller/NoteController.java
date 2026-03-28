package com.erp.client_service.controller;

import com.erp.client_service.dto.note.NoteDto;
import com.erp.client_service.service.note.NoteService;
import com.erp.client_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients/{clientId}/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final JwtUtil jwtUtil;


    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NoteDto> addNote(
            @PathVariable Long clientId,
            @RequestBody NoteDto dto,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth
    ) {
        String createdBy = jwtUtil.extractSubject(auth.substring(7));
        NoteDto saved = noteService.addNote(clientId, dto, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<NoteDto> listNotes(@PathVariable Long clientId) {
        return noteService.listNotes(clientId);
    }

    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteNote(@PathVariable Long noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NoteDto> updateNote(@PathVariable Long noteId, @RequestBody NoteDto dto) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(noteService.updateNode(noteId, dto));
    }

}
