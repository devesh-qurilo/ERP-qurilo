package com.erp.finance_servic.entity.receipt;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice_receipt")
public class InvoiceReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String invoiceId;
    private LocalDate issueDate;
    private String currency;
    //Seller details
    private String sellerCompanyName;
    private String sellerCompanyAddress;
    private String sellerCompanyCode;
    private String sellerCompanyTaxNumber;
    private String sellerCompanyEmail;
    private String sellerCompanyPhoneNumber;
    private String sellerCompanyBankName;
    private String sellerCompanyBankAccountNumber;

    //Buyer details
    private String buyerCompanyName;
    private String buyerCompanyAddress;
    private String buyerCompanyCode;
    private String buyerCompanyTaxNumber;
    private String buyerCleintName;
    private String buyerCompanyEmail;
    private String buyerCompanyPhoneNumber;
    private String buyerCompanyBankName;
    private String buyerCompanyBankAccountNumber;

    //List
    private String productName;
    private Long tax;
    private BigDecimal priceWithOutTax;
    private Long quantity;
    private BigDecimal totalExcluidingTax;
    private BigDecimal subtotal;
    private BigDecimal taxTotal;
    private BigDecimal totalAmount;

    //Notes
    private String description;
    private String totalAmountInWord;
    private String invoiceIssuedBy;

    @CreationTimestamp
    private LocalDate createdAt;
}
