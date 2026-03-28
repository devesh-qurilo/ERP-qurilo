package com.erp.project_service.mapper;

import com.erp.project_service.dto.note.TaskNoteDto;
import com.erp.project_service.entity.ProjectNote;
import com.erp.project_service.entity.TaskNote;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class NoteMapper {

    public static TaskNoteDto toDto(TaskNote note) {
        if (note == null) return null;

        TaskNoteDto dto = new TaskNoteDto();
        dto.setId(note.getId());
        dto.setTaskId(note.getTaskId());
        dto.setTitle(note.getTitle());
        dto.setContent(note.getContent());
        dto.setIsPublic(note.getIsPublic());
        dto.setOwnerEmployeeId(note.getOwnerEmployeeId());
        dto.setCreatedBy(note.getCreatedBy());

        if (note.getCreatedDate() != null) {
            dto.setCreatedAt(note.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant());
        }

        return dto;
    }

    public static TaskNoteDto toDto(ProjectNote note) {
        if (note == null) return null;

        TaskNoteDto dto = new TaskNoteDto();
        dto.setId(note.getId());
        dto.setTitle(note.getTitle());
        dto.setContent(note.getContent());
        dto.setIsPublic(note.getIsPublic());
        dto.setOwnerEmployeeId(note.getOwnerEmployeeId());
        dto.setCreatedBy(note.getCreatedBy());

        if (note.getCreatedDate() != null) {
            dto.setCreatedAt(note.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant());
        }

        return dto;
    }
}