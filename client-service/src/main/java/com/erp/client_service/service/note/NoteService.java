package com.erp.client_service.service.note;

import com.erp.client_service.dto.note.NoteDto;

import java.util.List;

public interface NoteService {
    NoteDto addNote(Long clientId, NoteDto dto, String createdBy);
    List<NoteDto> listNotes(Long clientId);
    void deleteNote(Long noteId);

    NoteDto updateNode(Long noteId, NoteDto dto);
}