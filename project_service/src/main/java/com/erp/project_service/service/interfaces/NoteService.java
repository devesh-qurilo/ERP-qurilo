package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.note.TaskNoteCreateRequest;
import com.erp.project_service.dto.note.TaskNoteDto;
import com.erp.project_service.dto.note.TaskNoteUpdateRequest;

import java.util.List;

public interface NoteService {
    // Task notes
    TaskNoteDto createTaskNote(TaskNoteCreateRequest req, String createdBy);
    List<TaskNoteDto> listTaskNotes(Long taskId, String requesterId);
    void deleteTaskNote(Long noteId, String actor);

    // Project notes
    TaskNoteDto createProjectNote(Long projectId, com.erp.project_service.dto.note.TaskNoteCreateRequest req, String createdBy);
    List<TaskNoteDto> listProjectNotes(Long projectId, String requesterId);
    void deleteProjectNote(Long noteId, String actor);

    TaskNoteDto updateTaskNote(Long noteId, TaskNoteUpdateRequest req, String actor);

    TaskNoteDto updateProjectNote(Long noteId, TaskNoteUpdateRequest req, String actor);
}
