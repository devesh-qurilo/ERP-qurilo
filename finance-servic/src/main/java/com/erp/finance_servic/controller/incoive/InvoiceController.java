package com.erp.finance_servic.controller.incoive;

import com.erp.finance_servic.dto.invoice.request.InvoiceCreateRequest;
import com.erp.finance_servic.dto.invoice.request.InvoiceUpdateRequest;
import com.erp.finance_servic.dto.invoice.response.InvoiceResponse;
import com.erp.finance_servic.dto.invoice.response.UnpaidInvoiceStatsDto;
import com.erp.finance_servic.service.invoice.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@RequestBody InvoiceCreateRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable String id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        List<InvoiceResponse> responses = invoiceService.getAllInvoices();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<Void> markInvoiceAsPaid(@PathVariable String id) {
        invoiceService.markInvoiceAsPaid(id);
        return ResponseEntity.ok().build();
    }

    // File endpoints (separate from createInvoice per your spec)
    @PostMapping("/{id}/files")
    public ResponseEntity<String> uploadInvoiceFile(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        String url = invoiceService.uploadInvoiceFile(id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }

    @DeleteMapping("/{id}/files")
    public ResponseEntity<Void> deleteInvoiceFile(@PathVariable String id, @RequestParam("fileUrl") String fileUrl) {
        invoiceService.deleteInvoiceFile(id, fileUrl);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> updateInvoice(@PathVariable String invoiceNumber,
                                                         @RequestBody InvoiceUpdateRequest updateRequest) {
        InvoiceResponse updated = invoiceService.updateInvoice(invoiceNumber, updateRequest);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete invoice by invoice number. This will:
     *  - delete all stored files related to the invoice from Supabase
     *  - delete invoice (and cascade-delete payments / credit notes if mapped)
     */
    @DeleteMapping("/{invoiceNumber}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable String invoiceNumber) {
        invoiceService.deleteInvoice(invoiceNumber);
        return ResponseEntity.noContent().build();
    }


    /**
     * ACTION: Send payment reminder email for UNPAID invoice.
     * No business logic change to invoice — pure side effect (email).
     */
    @PostMapping("/{invoiceNumber}/actions/send-reminder-email")
    public ResponseEntity<Void> sendUnpaidReminder(@PathVariable String invoiceNumber) {
        invoiceService.sendUnpaidReminderEmail(invoiceNumber);
        // 202 Accepted to indicate side-effect triggered
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Admin-only: get invoices for any client.
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByClientForAdmin(@PathVariable String clientId) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByClientId(clientId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Admin-only: get invoices for any project.
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByProjectForAdmin(@PathVariable String projectId) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByProjectId(projectId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/client/{clientId}/stats/unpaid")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UnpaidInvoiceStatsDto> getUnpaidStatsByClient(@PathVariable String clientId) {
        UnpaidInvoiceStatsDto stats = invoiceService.getUnpaidInvoiceStatsByClient(clientId);
        return ResponseEntity.ok(stats);
    }

}
