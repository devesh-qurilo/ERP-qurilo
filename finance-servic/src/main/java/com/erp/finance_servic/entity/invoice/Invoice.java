package com.erp.finance_servic.entity.invoice;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber;

    private LocalDate invoiceDate;
    private String currency;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String clientId;

    private BigDecimal projectBudget;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    private BigDecimal amount;
    private BigDecimal tax; // treated as percentage (e.g., 18 for 18%)
    private BigDecimal discount; // percentage

    @Column(nullable = false)
    private BigDecimal total;

    private String amountInWords;
    private String notes;

    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private BigDecimal adjustment;

    @ElementCollection
    private List<String> fileUrls = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<CreditNote> creditNotes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateAmounts();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateAmounts();
    }

    private void calculateAmounts() {
        if (amount == null) amount = BigDecimal.ZERO;
        if (discount == null) discount = BigDecimal.ZERO;
        if (tax == null) tax = BigDecimal.ZERO;
        if (adjustment == null) adjustment = BigDecimal.ZERO;
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;

        // discount amount (percentage)
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = amount.multiply(discount).divide(BigDecimal.valueOf(100));
        }

        // taxable base after discount
        BigDecimal taxable = amount.subtract(discountAmount);

        // tax treated as percentage
        BigDecimal taxAmount = BigDecimal.ZERO;
        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = taxable.multiply(tax).divide(BigDecimal.valueOf(100));
        }

        // total = taxable + taxAmount + adjustment
        BigDecimal computedTotal = taxable.add(taxAmount).add(adjustment == null ? BigDecimal.ZERO : adjustment);

        this.total = computedTotal.setScale(2, java.math.RoundingMode.HALF_EVEN);

        // unpaid = total - paid
        BigDecimal computedPaid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal computedUnpaid = this.total.subtract(computedPaid).setScale(2, java.math.RoundingMode.HALF_EVEN);
        if (computedUnpaid.compareTo(BigDecimal.ZERO) < 0) {
            // policy: do not store negative unpaid; set to zero (could also store credit separately)
            computedUnpaid = BigDecimal.ZERO;
        }
        this.unpaidAmount = computedUnpaid;

        // status rules (keep CREDIT_NOTES if already set)
        if (this.unpaidAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.status = InvoiceStatus.PAID;
        } else if (this.status != InvoiceStatus.CREDIT_NOTES) {
            this.status = InvoiceStatus.UNPAID;
        }

        // IMPORTANT: Automatic status update ONLY if not already manually set to PAID
        // Agar manually PAID set kiya gaya hai, to use preserve karo
        if (this.status != InvoiceStatus.PAID) {
            if (this.unpaidAmount.compareTo(BigDecimal.ZERO) == 0) {
                this.status = InvoiceStatus.PAID;
            } else if (this.status != InvoiceStatus.CREDIT_NOTES) {
                this.status = InvoiceStatus.UNPAID;
            }
        }
    }
}
