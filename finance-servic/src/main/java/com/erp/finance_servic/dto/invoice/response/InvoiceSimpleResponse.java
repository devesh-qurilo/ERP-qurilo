package com.erp.finance_servic.dto.invoice.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSimpleResponse {
    private Long id;
    private String invoiceNumber;
    private BigDecimal total;
    private String status;
}
