package com.erp.finance_servic.dto.invoice.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial update request for invoices.
 * Fields are nullable — only non-null values will be applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceUpdateRequest {
    private LocalDate invoiceDate;
    private String currency;
    private BigDecimal amount;
    private BigDecimal tax;
    private BigDecimal discount; // percentage 0..100
    private String amountInWords;
    private String notes;
    // We intentionally do NOT accept status, paidAmount, unpaidAmount, payments, creditNotes here.
}
