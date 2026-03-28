package com.erp.finance_servic.dto.invoice.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditNoteResponse {
    private Long id;
    private String creditNoteNumber;
    private String invoiceNumber;
    private LocalDate creditNoteDate;
    private String currency;
    private BigDecimal adjustment;
    private Boolean adjustmentPositive;
    private BigDecimal tax;
    private BigDecimal amount;
    private String notes;
    private String fileUrl;
    private ClientResponse client;
    private ProjectResponse project;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
