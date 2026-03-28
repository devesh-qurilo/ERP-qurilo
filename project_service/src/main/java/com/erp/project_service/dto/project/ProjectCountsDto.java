package com.erp.project_service.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCountsDto {
    private Long pendingCount;
    private Long overdueCount;
}
