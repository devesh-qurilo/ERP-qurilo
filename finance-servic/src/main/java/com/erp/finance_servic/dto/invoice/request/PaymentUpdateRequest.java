package com.erp.finance_servic.dto.invoice.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Partial update for payment (no file replacement here).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentUpdateRequest {
    private BigDecimal amount;
    private String currency;
    private String transactionId;
    private Long paymentGatewayId;
    private String status; // PENDING | COMPLETED | FAILED | CANCELLED
    private String notes;
}
