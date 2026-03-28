package com.erp.finance_servic.dto.invoice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnpaidInvoiceStatsDto {
    private long unpaidInvoiceCount;
    private BigDecimal totalUnpaidAmount;
}
