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
public class CreditNoteCreateRequest {
    private String creditNoteNumber;
    private LocalDate creditNoteDate;
    private String currency;
    private BigDecimal adjustment;
    private Boolean adjustmentPositive;
    private BigDecimal tax;    // percentage (e.g. 18 for 18%)
    private String notes;
    private BigDecimal amount;
    // fileUrl omitted because file will be uploaded via multipart file part
    // amount is intentionally NOT present (server calculates it)
}
