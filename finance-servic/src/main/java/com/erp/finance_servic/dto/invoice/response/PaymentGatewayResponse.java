package com.erp.finance_servic.dto.invoice.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentGatewayResponse {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
}
