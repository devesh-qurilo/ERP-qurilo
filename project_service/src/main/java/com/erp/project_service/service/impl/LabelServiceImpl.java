package com.erp.project_service.service.impl;

import com.erp.project_service.dto.task.LabelDto;
import com.erp.project_service.entity.Label;
import com.erp.project_service.entity.Project;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.mapper.LabelMapper;
import com.erp.project_service.repository.LabelRepository;
import com.erp.project_service.repository.ProjectRepository;
import com.erp.project_service.service.interfaces.LabelService;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {

    private final LabelRepository repo;
    private final ProjectRepository projectRepository;
    private final ProjectActivityService activityService;

    @Override
    @Transactional
    public LabelDto create(LabelDto dto, String createdBy) {
        Label e = Label.builder()
                .name(dto.getName())
                .colorCode(dto.getColorCode())
                .projectId(dto.getProjectId())
                .description(dto.getDescription())
                .createdBy(createdBy)
                .build();
        Label saved = repo.save(e);
        activityService.record(e.getProjectId(), createdBy, "LABEL_CREATED", String.valueOf(saved.getId()));
        return enrich(LabelMapper.toDto(saved)); // ✅ Add enrichment
    }

    @Override
    @Transactional
    public LabelDto update(Long id, LabelDto dto, String updatedBy) {
        Label e = repo.findById(id).orElseThrow(() -> new NotFoundException("Label not found"));
        e.setName(dto.getName() != null ? dto.getName() : e.getName());
        e.setColorCode(dto.getColorCode() != null ? dto.getColorCode() : e.getColorCode());
        e.setDescription(dto.getDescription() != null ? dto.getDescription() : e.getDescription());
        e.setUpdatedBy(updatedBy);
        Label saved = repo.save(e);
        activityService.record(e.getProjectId(), updatedBy, "LABEL_UPDATED", String.valueOf(saved.getId()));
        return enrich(LabelMapper.toDto(saved)); // ✅ Add enrichment
    }

    @Override
    @Transactional
    public void delete(Long id, String deletedBy) {
        Label e = repo.findById(id).orElseThrow(() -> new NotFoundException("Label not found"));
        repo.deleteById(id);
        activityService.record(e.getProjectId(), deletedBy, "LABEL_DELETED", String.valueOf(id));
    }

    @Override
    public List<LabelDto> listByProject(Long projectId) {
        return repo.findByProjectId(projectId).stream()
                .map(LabelMapper::toDto)
                .map(this::enrich) // ✅ Add enrichment
                .collect(Collectors.toList());
    }

    @Override
    public List<LabelDto> listAll() {
        return repo.findAll().stream()
                .map(LabelMapper::toDto)
                .map(this::enrich)   // ✅ ADD THIS
                .collect(Collectors.toList());
    }

    // ✅ Add enrichment method to populate projectName
    private LabelDto enrich(LabelDto dto) {
        if (dto == null || dto.getProjectId() == null) return dto;

        try {
            Project project = projectRepository.findById(dto.getProjectId()).orElse(null);
            if (project != null) {
                dto.setProjectName(project.getName());
            }
        } catch (Exception e) {
            // Log error but don't fail the request
            System.err.println("Failed to fetch project for label enrichment: " + e.getMessage());
        }

        return dto;
    }
}