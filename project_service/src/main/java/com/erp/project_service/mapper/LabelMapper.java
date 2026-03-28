package com.erp.project_service.mapper;

import com.erp.project_service.dto.task.LabelDto;
import com.erp.project_service.entity.Label;

public class LabelMapper {

    public static LabelDto toDto(Label label) {
        if (label == null) return null;

        LabelDto dto = new LabelDto();
        dto.setId(label.getId());
        dto.setName(label.getName());
        dto.setColorCode(label.getColorCode());
        dto.setProjectId(label.getProjectId());
        dto.setDescription(label.getDescription());
        dto.setCreatedBy(label.getCreatedBy());
        // projectName will be set during enrichment
        return dto;
    }

    public static Label toEntity(LabelDto dto) {
        if (dto == null) return null;

        return Label.builder()
                .name(dto.getName())
                .colorCode(dto.getColorCode())
                .projectId(dto.getProjectId())
                .description(dto.getDescription())
                .createdBy(dto.getCreatedBy())
                .build();
    }
}