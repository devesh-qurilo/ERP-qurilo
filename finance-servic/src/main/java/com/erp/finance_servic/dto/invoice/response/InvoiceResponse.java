package com.erp.finance_servic.dto.invoice.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String currency;
    private ClientResponse client;
    private ProjectResponse project;
    private BigDecimal projectBudget;
    private String status;
    private BigDecimal amount;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal total;
    private String amountInWords;
    private String notes;
    private List<String> fileUrls;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private BigDecimal adjustment;
    private LocalDateTime createdAt;
}
