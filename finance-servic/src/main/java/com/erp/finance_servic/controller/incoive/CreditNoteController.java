package com.erp.finance_servic.controller.incoive;

import com.erp.finance_servic.dto.invoice.request.CreditNoteCreateRequest;
import com.erp.finance_servic.dto.invoice.request.CreditNoteUpdateRequest;
import com.erp.finance_servic.dto.invoice.response.CreditNoteResponse;
import com.erp.finance_servic.entity.invoice.CreditNote;
import com.erp.finance_servic.service.invoice.CreditNoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class CreditNoteController {

    private final CreditNoteService creditNoteService;
    private final ObjectMapper objectMapper;

    /**
     * Create Credit Note for a given invoiceNumber (path variable).
     *
     * multipart/form-data:
     *  - part "creditNote" -> JSON string (CreditNoteCreateRequest)
     *  - part "file" -> optional file to attach
     */
    @PostMapping(value = "/invoices/{invoiceNumber}/credit-notes", consumes = {"multipart/form-data"})
    public ResponseEntity<CreditNoteResponse> createForInvoice(
            @PathVariable("invoiceNumber") String invoiceNumber,
            @RequestPart("creditNote") String creditNoteJson,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws Exception {
        CreditNoteCreateRequest req = objectMapper.readValue(creditNoteJson, CreditNoteCreateRequest.class);
        CreditNoteResponse resp = creditNoteService.createCreditNoteForInvoice(invoiceNumber, req, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/invoices/{invoiceNumber}/credit-notes")
    public ResponseEntity<List<CreditNoteResponse>> listByInvoiceNumber(@PathVariable("invoiceNumber") String invoiceNumber) {
        return ResponseEntity.ok(creditNoteService.getCreditNotesByInvoiceNumber(invoiceNumber));
    }

    @PostMapping(value = "/credit-notes/{creditNoteId}/files", consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadFile(@PathVariable Long creditNoteId,
                                             @RequestParam("file") MultipartFile file) {
        String url = creditNoteService.uploadCreditNoteFile(creditNoteId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }

    @PutMapping("/credit-notes/{creditNoteId}")
    public ResponseEntity<CreditNoteResponse> update(@PathVariable Long creditNoteId,
                                                     @RequestBody CreditNoteUpdateRequest request) {
        CreditNoteResponse resp = creditNoteService.updateCreditNote(creditNoteId, request);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/credit-notes/{creditNoteId}")
    public ResponseEntity<Void> delete(@PathVariable Long creditNoteId) {
        creditNoteService.deleteCreditNote(creditNoteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/credit-notes/getAll")
    public ResponseEntity<List<CreditNoteResponse>> getAll() {

        return ResponseEntity.ok(creditNoteService.getAll());
    }

    /**
     * Admin-only: get credit notes for any client.
     */
    @GetMapping("/credit-notes/client/{clientId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<CreditNoteResponse>> getCreditNotesByClientForAdmin(@PathVariable String clientId) {
        List<CreditNoteResponse> list = creditNoteService.getCreditNotesByClientId(clientId);
        return ResponseEntity.ok(list);
    }
}
