package com.erp.finance_servic.controller.incoive;

import com.erp.finance_servic.dto.invoice.request.PaymentCreateRequest;
import com.erp.finance_servic.dto.invoice.request.PaymentUpdateRequest;
import com.erp.finance_servic.dto.invoice.response.PaymentResponse;
import com.erp.finance_servic.service.invoice.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    /**
     * Create payment (multipart/form-data)
     * - part "payment": JSON string matching PaymentCreateRequest
     * - part "file": optional MultipartFile
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestPart("payment") String paymentJson,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws Exception {
        PaymentCreateRequest req = objectMapper.readValue(paymentJson, PaymentCreateRequest.class);
        PaymentResponse response = paymentService.createPaymentWithReceipt(req, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoice(@PathVariable String invoiceNumber) {
        List<PaymentResponse> responses = paymentService.getPaymentsByInvoiceNumber(invoiceNumber);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByClient(@PathVariable String clientId) {
        List<PaymentResponse> responses = paymentService.getPaymentsByClientId(clientId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByProject(@PathVariable String projectId) {
        List<PaymentResponse> responses = paymentService.getPaymentsByProjectId(projectId);
        return ResponseEntity.ok(responses);
    }

    // Upload payment receipt (separate endpoint) - form-data (multipart/form-data)
    @PostMapping("/{paymentId}/receipt")
    public ResponseEntity<String> uploadPaymentReceipt(@PathVariable Long paymentId,
                                                       @RequestParam("file") MultipartFile file) {
        String url = paymentService.uploadPaymentReceipt(paymentId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(url);
    }

    // Update payment (partial) - JSON body
    @PutMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> updatePayment(@PathVariable Long paymentId,
                                                         @RequestBody PaymentUpdateRequest request) {
        PaymentResponse updated = paymentService.updatePayment(paymentId, request);
        return ResponseEntity.ok(updated);
    }

    // Delete payment (also deletes receipt file if present)
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.noContent().build();
    }

}
