package com.erp.finance_servic.service.receipt;

import com.erp.finance_servic.entity.receipt.InvoiceReceipt;
import com.erp.finance_servic.dto.receipt.RequestInvoiceReceipt;
import com.erp.finance_servic.dto.receipt.ResponseInvoiceReceipt;
import com.erp.finance_servic.repository.receipt.InvoiceReceiptRepository;
import com.erp.finance_servic.util.AmountToWordConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceReceiptService {

    private final InvoiceReceiptRepository invoiceReceiptRepository;
    private final AmountToWordConverter amountToWordConverter;

    public ResponseInvoiceReceipt createInvoice(RequestInvoiceReceipt request) {
        BigDecimal totalExcluidingTax = calculateTotalExcludingTax(request.getPriceWithOutTax(), request.getQuantity());
        BigDecimal taxTotal = calculateTaxTotal(totalExcluidingTax, request.getTax());
        BigDecimal totalAmount = calculateTotalAmount(totalExcluidingTax, taxTotal);

        InvoiceReceipt invoice = new InvoiceReceipt();
        // Basic
        invoice.setInvoiceId(request.getInvoiceId());
        invoice.setIssueDate(request.getIssueDate());
        invoice.setCurrency(request.getCurrency());

        // Seller
        invoice.setSellerCompanyName(request.getSellerCompanyName());
        invoice.setSellerCompanyAddress(request.getSellerCompanyAddress());
        invoice.setSellerCompanyCode(request.getSellerCompanyCode());
        invoice.setSellerCompanyTaxNumber(request.getSellerCompanyTaxNumber());
        invoice.setSellerCompanyEmail(request.getSellerCompanyEmail());
        invoice.setSellerCompanyPhoneNumber(request.getSellerCompanyPhoneNumber());
        invoice.setSellerCompanyBankName(request.getSellerCompanyBankName());
        invoice.setSellerCompanyBankAccountNumber(request.getSellerCompanyBankAccountNumber());

        // Buyer
        invoice.setBuyerCompanyName(request.getBuyerCompanyName());
        invoice.setBuyerCompanyAddress(request.getBuyerCompanyAddress());
        invoice.setBuyerCompanyCode(request.getBuyerCompanyCode());
        invoice.setBuyerCompanyTaxNumber(request.getBuyerCompanyTaxNumber());
        invoice.setBuyerCleintName(request.getBuyerCleintName());
        invoice.setBuyerCompanyEmail(request.getBuyerCompanyEmail());
        invoice.setBuyerCompanyPhoneNumber(request.getBuyerCompanyPhoneNumber());
        invoice.setBuyerCompanyBankName(request.getBuyerCompanyBankName());
        invoice.setBuyerCompanyBankAccountNumber(request.getBuyerCompanyBankAccountNumber());

        // Product
        invoice.setProductName(request.getProductName());
        invoice.setTax(request.getTax());
        invoice.setPriceWithOutTax(request.getPriceWithOutTax());
        invoice.setQuantity(request.getQuantity());

        // Calculated
        invoice.setTotalExcluidingTax(totalExcluidingTax);
        invoice.setSubtotal(totalExcluidingTax);
        invoice.setTaxTotal(taxTotal);
        invoice.setTotalAmount(totalAmount);

        // Notes
        invoice.setDescription(request.getDescription());
        invoice.setTotalAmountInWord(amountToWordConverter.convert(totalAmount, request.getCurrency()));
        invoice.setInvoiceIssuedBy(request.getSellerCompanyName());

        InvoiceReceipt saved = invoiceReceiptRepository.save(invoice);
        return mapToResponse(saved);
    }

    public ResponseInvoiceReceipt getById(Long id) {
        return invoiceReceiptRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("receipt not found with id: " + id));
    }

    public List<ResponseInvoiceReceipt> getAllInvoices() {
        return invoiceReceiptRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ResponseInvoiceReceipt updateInvoice(Long id, RequestInvoiceReceipt request) {
        InvoiceReceipt ex = invoiceReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        BigDecimal totalExcluidingTax = calculateTotalExcludingTax(request.getPriceWithOutTax(), request.getQuantity());
        BigDecimal taxTotal = calculateTaxTotal(totalExcluidingTax, request.getTax());
        BigDecimal totalAmount = calculateTotalAmount(totalExcluidingTax, taxTotal);

        ex.setInvoiceId(request.getInvoiceId());
        ex.setIssueDate(request.getIssueDate());
        ex.setCurrency(request.getCurrency());
        ex.setSellerCompanyName(request.getSellerCompanyName());
        ex.setSellerCompanyAddress(request.getSellerCompanyAddress());
        ex.setBuyerCompanyName(request.getBuyerCompanyName());
        ex.setBuyerCompanyAddress(request.getBuyerCompanyAddress());
        ex.setProductName(request.getProductName());
        ex.setTax(request.getTax());
        ex.setPriceWithOutTax(request.getPriceWithOutTax());
        ex.setQuantity(request.getQuantity());
        ex.setTotalExcluidingTax(totalExcluidingTax);
        ex.setSubtotal(totalExcluidingTax);
        ex.setTaxTotal(taxTotal);
        ex.setTotalAmount(totalAmount);
        ex.setTotalAmountInWord(amountToWordConverter.convert(totalAmount, request.getCurrency()));
        ex.setDescription(request.getDescription());

        return mapToResponse(invoiceReceiptRepository.save(ex));
    }

    public void deleteInvoice(Long id) {
        invoiceReceiptRepository.deleteById(id);
    }

    private BigDecimal calculateTotalExcludingTax(BigDecimal price, Long quantity) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
    private BigDecimal calculateTaxTotal(BigDecimal totalExcludingTax, Long taxPercentage) {
        return totalExcludingTax.multiply(BigDecimal.valueOf(taxPercentage)).divide(BigDecimal.valueOf(100));
    }
    private BigDecimal calculateTotalAmount(BigDecimal totalExcludingTax, BigDecimal taxTotal) {
        return totalExcludingTax.add(taxTotal);
    }

    private ResponseInvoiceReceipt mapToResponse(InvoiceReceipt inv) {
        ResponseInvoiceReceipt r = new ResponseInvoiceReceipt();
        r.setId(inv.getId());
        r.setInvoiceId(inv.getInvoiceId());
        r.setIssueDate(inv.getIssueDate());
        r.setCurrency(inv.getCurrency());
        // Seller
        r.setSellerCompanyName(inv.getSellerCompanyName());
        r.setSellerCompanyAddress(inv.getSellerCompanyAddress());
        r.setSellerCompanyCode(inv.getSellerCompanyCode());
        r.setSellerCompanyTaxNumber(inv.getSellerCompanyTaxNumber());
        r.setSellerCompanyEmail(inv.getSellerCompanyEmail());
        r.setSellerCompanyPhoneNumber(inv.getSellerCompanyPhoneNumber());
        r.setSellerCompanyBankName(inv.getSellerCompanyBankName());
        r.setSellerCompanyBankAccountNumber(inv.getSellerCompanyBankAccountNumber());
        // Buyer
        r.setBuyerCompanyName(inv.getBuyerCompanyName());
        r.setBuyerCompanyAddress(inv.getBuyerCompanyAddress());
        r.setBuyerCompanyCode(inv.getBuyerCompanyCode());
        r.setBuyerCompanyTaxNumber(inv.getBuyerCompanyTaxNumber());
        r.setBuyerCleintName(inv.getBuyerCleintName());
        r.setBuyerCompanyEmail(inv.getBuyerCompanyEmail());
        r.setBuyerCompanyPhoneNumber(inv.getBuyerCompanyPhoneNumber());
        r.setBuyerCompanyBankName(inv.getBuyerCompanyBankName());
        r.setBuyerCompanyBankAccountNumber(inv.getBuyerCompanyBankAccountNumber());
        // Product + calc
        r.setProductName(inv.getProductName());
        r.setTax(inv.getTax());
        r.setPriceWithOutTax(inv.getPriceWithOutTax());
        r.setQuantity(inv.getQuantity());
        r.setTotalExcluidingTax(inv.getTotalExcluidingTax());
        r.setSubtotal(inv.getSubtotal());
        r.setTaxTotal(inv.getTaxTotal());
        r.setTotalAmount(inv.getTotalAmount());
        // Notes
        r.setDescription(inv.getDescription());
        r.setTotalAmountInWord(inv.getTotalAmountInWord()); // already currency-aware
        r.setInvoiceIssuedBy(inv.getInvoiceIssuedBy());
        r.setCreatedAt(inv.getCreatedAt());
        return r;
    }

    public List<ResponseInvoiceReceipt> getByInvoiceId(String invoidId) {
        List<InvoiceReceipt> receipts = invoiceReceiptRepository.findByInvoiceId(invoidId);
              return receipts.stream()
                .map(this::mapToResponse)
                      .collect(Collectors.toList());
    }
}
