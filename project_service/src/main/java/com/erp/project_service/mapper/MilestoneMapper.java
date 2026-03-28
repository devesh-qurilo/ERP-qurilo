package com.erp.project_service.mapper;

import com.erp.project_service.dto.milestone.MilestoneCreateRequest;
import com.erp.project_service.dto.milestone.MilestoneDto;
import com.erp.project_service.entity.MilestoneStatus;
import com.erp.project_service.entity.ProjectMilestone;

public final class MilestoneMapper {
    private MilestoneMapper() {}

    public static ProjectMilestone toEntity(MilestoneCreateRequest r) {
        if (r == null) return null;
        ProjectMilestone m = ProjectMilestone.builder()
                .title(r.getTitle())
                .milestoneCost(r.getMilestoneCost())
                .status(r.getStatus() == null ? MilestoneStatus.INCOMPLETE: com.erp.project_service.entity.MilestoneStatus.valueOf(r.getStatus()))
                .summary(r.getSummary())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .build();
        return m;
    }

    public static MilestoneDto toDto(ProjectMilestone e) {
        if (e == null) return null;
        MilestoneDto dto = new MilestoneDto();
        dto.setId(e.getId());
        dto.setProjectId(e.getProjectId());
        dto.setTitle(e.getTitle());
        dto.setMilestoneCost(e.getMilestoneCost());
        dto.setStatus(e.getStatus() == null? null : e.getStatus().name());
        dto.setSummary(e.getSummary());
        dto.setStartDate(e.getStartDate());
        dto.setEndDate(e.getEndDate());
        dto.setCreatedBy(e.getCreatedBy());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedBy(e.getUpdatedBy());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}
