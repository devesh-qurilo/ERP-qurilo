package com.erp.project_service.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientProjectStatsDto {
    private long projectCount;
    private BigDecimal totalEarning;
}
