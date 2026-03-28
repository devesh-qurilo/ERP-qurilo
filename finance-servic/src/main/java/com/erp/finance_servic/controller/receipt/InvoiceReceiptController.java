package com.erp.finance_servic.controller.receipt;

import com.erp.finance_servic.dto.receipt.RequestInvoiceReceipt;
import com.erp.finance_servic.dto.receipt.ResponseInvoiceReceipt;
import com.erp.finance_servic.service.receipt.InvoicePdfService;
import com.erp.finance_servic.service.receipt.InvoiceReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceReceiptController {

    private final InvoiceReceiptService invoiceReceiptService;
    private final InvoicePdfService invoicePdfService;

    @PostMapping
    public ResponseEntity<ResponseInvoiceReceipt> createInvoice(@RequestBody RequestInvoiceReceipt request) {
        return ResponseEntity.ok(invoiceReceiptService.createInvoice(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseInvoiceReceipt> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceReceiptService.getById(id));
    }

    @GetMapping("/receipt/{invoidId}")
    public ResponseEntity<List<ResponseInvoiceReceipt>> getInvoiceReceipt(@PathVariable String invoidId) {
        return ResponseEntity.ok(invoiceReceiptService.getByInvoiceId(invoidId));
    }

    @GetMapping
    public ResponseEntity<List<ResponseInvoiceReceipt>> getAllInvoices() {
        return ResponseEntity.ok(invoiceReceiptService.getAllInvoices());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseInvoiceReceipt> updateInvoice(
            @PathVariable Long id,
            @RequestBody RequestInvoiceReceipt request) {
        return ResponseEntity.ok(invoiceReceiptService.updateInvoice(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceReceiptService.deleteInvoice(id);
        return ResponseEntity.ok().build();
    }

    // ✅ Download as PDF
    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           @RequestParam(defaultValue = "attachment") String disposition) {
        byte[] pdf = invoicePdfService.render(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"Invoice-" + id + ".pdf\"")
                .body(pdf);
    }
}
