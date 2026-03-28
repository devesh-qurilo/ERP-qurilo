package com.erp.finance_servic.dto.invoice.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {
    private String projectId;
    private String clientId;
    private String currency;
    private BigDecimal amount;
    private String transactionId;
    private String invoiceId;
    private Long paymentGatewayId;
    private MultipartFile receiptFileUrl;
    private String notes;
}
