package com.erp.finance_servic.dto.invoice.request;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreateRequest {
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String currency;
    private String projectId;
    private String clientId;
    private BigDecimal amount;
    private BigDecimal tax;
    private BigDecimal discount;
    private String amountInWords;
    private String notes;
}
