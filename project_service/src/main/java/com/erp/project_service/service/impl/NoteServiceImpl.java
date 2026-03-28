package com.erp.project_service.service.impl;

import com.erp.project_service.dto.note.TaskNoteCreateRequest;
import com.erp.project_service.dto.note.TaskNoteDto;
import com.erp.project_service.dto.note.TaskNoteUpdateRequest;
import com.erp.project_service.entity.ProjectNote;
import com.erp.project_service.entity.TaskNote;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.mapper.NoteMapper;
import com.erp.project_service.repository.ProjectNoteRepository;
import com.erp.project_service.repository.TaskNoteRepository;
import com.erp.project_service.service.interfaces.NoteService;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final TaskNoteRepository taskNoteRepo;
    private final ProjectNoteRepository projectNoteRepo;
    private final ProjectActivityService activityService;

    @Override
    @Transactional
    public TaskNoteDto createTaskNote(TaskNoteCreateRequest req, String createdBy) {
        System.out.println("=== CREATING TASK NOTE ===");
        System.out.println("Task ID: " + req.getTaskId());
        System.out.println("Title: " + req.getTitle());
        System.out.println("Content: " + req.getContent());
        System.out.println("Created By: " + createdBy);

        TaskNote n = TaskNote.builder()
                .taskId(req.getTaskId())
                .title(req.getTitle())
                .content(req.getContent())
                .isPublic(req.getIsPublic() != null ? req.getIsPublic() : true)
                .ownerEmployeeId(createdBy)
                .createdBy(createdBy)
                .build();

        TaskNote saved = taskNoteRepo.save(n);
        System.out.println("✓ Task Note Saved with ID: " + saved.getId());

        activityService.record(null, createdBy, "TASK_NOTE_CREATED", String.valueOf(saved.getId()));
        return NoteMapper.toDto(saved);
    }

    @Override
    public List<TaskNoteDto> listTaskNotes(Long taskId, String requesterId) {
        System.out.println("=== LISTING TASK NOTES ===");
        System.out.println("Task ID: " + taskId);
        System.out.println("Requester: " + requesterId);

        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();
        List<TaskNote> all = taskNoteRepo.findByTaskId(taskId);

        System.out.println("Found " + all.size() + " notes total");
        System.out.println("Is Admin: " + isAdmin);

        List<TaskNoteDto> result = all.stream()
                .filter(n -> {
                    boolean hasAccess = n.getIsPublic() || isAdmin || requesterId.equals(n.getOwnerEmployeeId());
                    System.out.println("Note " + n.getId() + " - Public: " + n.getIsPublic() + ", Access: " + hasAccess);
                    return hasAccess;
                })
                .map(NoteMapper::toDto)
                .collect(Collectors.toList());

        System.out.println("Returning " + result.size() + " notes");
        return result;
    }

    @Override
    @Transactional
    public void deleteTaskNote(Long noteId, String actor) {
        System.out.println("=== DELETING TASK NOTE ===");
        System.out.println("Note ID: " + noteId);
        System.out.println("Actor: " + actor);

        TaskNote n = taskNoteRepo.findById(noteId).orElseThrow(() -> new NotFoundException("Note not found"));
        taskNoteRepo.deleteById(noteId);
        System.out.println("✓ Task Note Deleted: " + noteId);

        activityService.record(null, actor, "TASK_NOTE_DELETED", String.valueOf(noteId));
    }

    @Override
    @Transactional
    public TaskNoteDto createProjectNote(Long projectId, TaskNoteCreateRequest req, String createdBy) {
        System.out.println("=== CREATING PROJECT NOTE ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("Title: " + req.getTitle());
        System.out.println("Content: " + req.getContent());
        System.out.println("Created By: " + createdBy);

        ProjectNote n = ProjectNote.builder()
                .projectId(projectId)
                .title(req.getTitle())
                .content(req.getContent())
                .isPublic(req.getIsPublic() != null ? req.getIsPublic() : true)
                .ownerEmployeeId(createdBy)
                .createdBy(createdBy)
                .build();

        ProjectNote saved = projectNoteRepo.save(n);
        System.out.println("✓ Project Note Saved with ID: " + saved.getId());

        activityService.record(projectId, createdBy, "PROJECT_NOTE_CREATED", String.valueOf(saved.getId()));
        return NoteMapper.toDto(saved);
    }

    @Override
    public List<TaskNoteDto> listProjectNotes(Long projectId, String requesterId) {
        System.out.println("=== LISTING PROJECT NOTES ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("Requester: " + requesterId);

        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();
        List<ProjectNote> all = projectNoteRepo.findByProjectId(projectId);

        System.out.println("Found " + all.size() + " project notes total");
        System.out.println("Is Admin: " + isAdmin);

        List<TaskNoteDto> result = all.stream()
                .filter(n -> {
                    boolean hasAccess = n.getIsPublic() || isAdmin || requesterId.equals(n.getOwnerEmployeeId());
                    System.out.println("Project Note " + n.getId() + " - Public: " + n.getIsPublic() + ", Access: " + hasAccess);
                    return hasAccess;
                })
                .map(NoteMapper::toDto)
                .collect(Collectors.toList());

        System.out.println("Returning " + result.size() + " project notes");
        return result;
    }

    @Override
    @Transactional
    public void deleteProjectNote(Long noteId, String actor) {
        System.out.println("=== DELETING PROJECT NOTE ===");
        System.out.println("Note ID: " + noteId);
        System.out.println("Actor: " + actor);

        ProjectNote n = projectNoteRepo.findById(noteId).orElseThrow(() -> new NotFoundException("Note not found"));
        projectNoteRepo.deleteById(noteId);
        System.out.println("✓ Project Note Deleted: " + noteId);

        activityService.record(n.getProjectId(), actor, "PROJECT_NOTE_DELETED", String.valueOf(noteId));
    }

    @Override
    @Transactional
    public TaskNoteDto updateTaskNote(Long noteId, TaskNoteUpdateRequest req, String actor) {

        TaskNote note = taskNoteRepo.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Task note not found"));

        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();

        if (!isAdmin && !actor.equals(note.getOwnerEmployeeId())) {
            throw new RuntimeException("You are not allowed to edit this note");
        }

        if (req.getTitle() != null) note.setTitle(req.getTitle());
        if (req.getContent() != null) note.setContent(req.getContent());
        if (req.getIsPublic() != null) note.setPublic(req.getIsPublic());

        TaskNote saved = taskNoteRepo.save(note);

        activityService.record(
                null,
                actor,
                "TASK_NOTE_UPDATED",
                String.valueOf(noteId)
        );

        return NoteMapper.toDto(saved);
    }


    @Override
    @Transactional
    public TaskNoteDto updateProjectNote(Long noteId, TaskNoteUpdateRequest req, String actor) {

        ProjectNote note = projectNoteRepo.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Project note not found"));

        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();

        if (!isAdmin && !actor.equals(note.getOwnerEmployeeId())) {
            throw new RuntimeException("You are not allowed to edit this note");
        }

        if (req.getTitle() != null) note.setTitle(req.getTitle());
        if (req.getContent() != null) note.setContent(req.getContent());
        if (req.getIsPublic() != null) note.setPublic(req.getIsPublic());

        ProjectNote saved = projectNoteRepo.save(note);

        activityService.record(
                note.getProjectId(),
                actor,
                "PROJECT_NOTE_UPDATED",
                String.valueOf(noteId)
        );

        return NoteMapper.toDto(saved);
    }

}