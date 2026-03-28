package com.erp.lead_service.dto.lead;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadDealStatsDto {
    private Long leadId;
    private Long totalDeals;
    private Long convertedDeals; // number of deals which reached WIN
}
