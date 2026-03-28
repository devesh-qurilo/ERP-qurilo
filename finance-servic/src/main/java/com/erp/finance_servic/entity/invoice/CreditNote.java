package com.erp.finance_servic.entity.invoice;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String creditNoteNumber;

    private LocalDate creditNoteDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // NEW: store clientId for quick lookups
    @Column(name = "client_id", length = 255)
    private String clientId;


    private String currency;
    private BigDecimal adjustment;
    private Boolean adjustmentPositive;
    private BigDecimal tax;
    private BigDecimal amount;
    private String notes;
    private String fileUrl;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        creditNoteDate = LocalDate.now();
    }
}