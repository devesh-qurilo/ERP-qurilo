package com.erp.project_service.controller;

import com.erp.project_service.dto.note.TaskNoteCreateRequest;
import com.erp.project_service.dto.note.TaskNoteDto;
import com.erp.project_service.dto.note.TaskNoteUpdateRequest;
import com.erp.project_service.service.interfaces.NoteService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NoteController {

    private final NoteService svc;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping("/tasks/{taskId}/notes")
    public ResponseEntity<TaskNoteDto> createTaskNote(@PathVariable Long taskId, @RequestBody TaskNoteCreateRequest req) {
        String actor = SecurityUtils.getCurrentUserId();
        req.setTaskId(taskId);
        return ResponseEntity.status(201).body(svc.createTaskNote(req, actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/tasks/{taskId}/notes")
    public ResponseEntity<List<TaskNoteDto>> listTaskNotes(@PathVariable Long taskId) {
        String requester = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.listTaskNotes(taskId, requester));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping("/projects/{projectId}/notes")
    public ResponseEntity<TaskNoteDto> createProjectNote(@PathVariable Long projectId, @RequestBody TaskNoteCreateRequest req) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(201).body(svc.createProjectNote(projectId, req, actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/projects/{projectId}/notes")
    public ResponseEntity<List<TaskNoteDto>> listProjectNotes(@PathVariable Long projectId) {
        String requester = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.listProjectNotes(projectId, requester));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @DeleteMapping("/notes/task/{noteId}")
    public ResponseEntity<?> deleteTaskNote(@PathVariable Long noteId) {
        String actor = SecurityUtils.getCurrentUserId();
        svc.deleteTaskNote(noteId, actor); // works for both types
        return ResponseEntity.noContent().build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/notes/project/{noteId}")
    public ResponseEntity<?> deleteProjectNote(@PathVariable Long noteId) {
        String actor = SecurityUtils.getCurrentUserId();
        svc.deleteProjectNote(noteId, actor); // works for both types
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PutMapping("/notes/task/{noteId}")
    public ResponseEntity<TaskNoteDto> updateTaskNote(
            @PathVariable Long noteId,
            @RequestBody TaskNoteUpdateRequest req
    ) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.updateTaskNote(noteId, req, actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PutMapping("/notes/project/{noteId}")
    public ResponseEntity<TaskNoteDto> updateProjectNote(
            @PathVariable Long noteId,
            @RequestBody TaskNoteUpdateRequest req
    ) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.updateProjectNote(noteId, req, actor));
    }

}
