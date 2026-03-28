package com.erp.project_service.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCountsDto {
    private Long pendingCount;
    private Long overdueCount;
}
