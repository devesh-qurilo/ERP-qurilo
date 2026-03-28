package com.erp.finance_servic.dto.invoice.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditNoteUpdateRequest {
    private LocalDate creditNoteDate;
    private String currency;
    private BigDecimal adjustment;
    private Boolean adjustmentPositive;
    private BigDecimal tax;
    private BigDecimal amount;
    private String notes;
}
