package com.erp.lead_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowupSummaryDto {
    private long pendingCount;   // nextDate <= today and not completed/cancelled
    private long upcomingCount;  // nextDate > today and not completed/cancelled
}
