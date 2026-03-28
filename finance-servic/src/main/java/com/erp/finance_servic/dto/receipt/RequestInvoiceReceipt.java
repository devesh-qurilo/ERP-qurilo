package com.erp.finance_servic.dto.receipt;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RequestInvoiceReceipt {
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

    //notes
    private String description;
}
