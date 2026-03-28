package com.erp.finance_servic.dto.invoice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String projectId;
    private ProjectResponse project; // full project details
    private String clientId;
    private ClientResponse client;   // full client details
    private String currency;
    private BigDecimal amount;
    private String transactionId;
    private PaymentGatewayResponse paymentGateway;
    private String receiptFileUrl;
    private String status;
    private String note;
    private LocalDateTime paymentDate;
    private InvoiceSimpleResponse invoice;
}
