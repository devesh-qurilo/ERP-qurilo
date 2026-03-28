package com.erp.project_service.service.impl;

import com.erp.project_service.dto.task.SubtaskCreateRequest;
import com.erp.project_service.dto.task.SubtaskDto;
import com.erp.project_service.entity.Subtask;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.mapper.SubtaskMapper;
import com.erp.project_service.repository.SubtaskRepository;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import com.erp.project_service.service.interfaces.SubtaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubtaskServiceImpl implements SubtaskService {

    private final SubtaskRepository repo;
    private final ProjectActivityService activityService;

    @Override
    @Transactional
    public SubtaskDto create(SubtaskCreateRequest req, String createdBy) {
        Subtask s = SubtaskMapper.toEntity(req);
        s.setCreatedBy(createdBy);
        Subtask saved = repo.save(s);
        activityService.record(null, createdBy, "SUBTASK_CREATED", String.valueOf(saved.getId()));
        return SubtaskMapper.toDto(saved);
    }

    @Override
    public List<SubtaskDto> listByTask(Long taskId) {
        return repo.findByTaskId(taskId).stream().map(SubtaskMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubtaskDto update(Long id, SubtaskCreateRequest req, String updatedBy) {
        Subtask s = repo.findById(id).orElseThrow(() -> new NotFoundException("Subtask not found"));
        s.setTitle(req.getTitle() != null ? req.getTitle() : s.getTitle());
        s.setDescription(req.getDescription() != null ? req.getDescription() : s.getDescription());
        s.setUpdatedBy(updatedBy);
        Subtask saved = repo.save(s);
        activityService.record(s.getTaskId(), updatedBy, "SUBTASK_UPDATED", String.valueOf(saved.getId()));
        return SubtaskMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, String deletedBy) {
        Subtask s = repo.findById(id).orElseThrow(() -> new NotFoundException("Subtask not found"));
        repo.deleteById(id);
        activityService.record(s.getTaskId(), deletedBy, "SUBTASK_DELETED", String.valueOf(id));
    }

    @Override
    public void toggleStatus(Long subtaskId, String actor) {
        Subtask s = repo.findById(subtaskId).orElseThrow(() -> new NotFoundException("Subtask not found"));
        s.setDone(!s.isDone());
        activityService.record(null, actor, "SUBTASK_TOGGLED", String.valueOf(s.getId()));
    }
}
