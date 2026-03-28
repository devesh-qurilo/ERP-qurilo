package com.erp.finance_servic.entity.invoice;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Fixed: Join to Invoice using the invoiceNumber field
     * The referencedColumnName should match the field name in Invoice entity
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_number", referencedColumnName = "invoiceNumber", nullable = false)
    private Invoice invoice;

    private String projectId;
    private String clientId;
    private String currency;
    private BigDecimal amount;
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_gateway_id")
    private PaymentGatewayEntity paymentGateway;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String receiptFileUrl;

    private String note;

    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        paymentDate = LocalDateTime.now();
        this.status = PaymentStatus.COMPLETED;
    }

}

