package com.erp.finance_servic.controller.incoive;

import com.erp.finance_servic.dto.invoice.request.PaymentGatewayRequest;
import com.erp.finance_servic.dto.invoice.response.PaymentGatewayResponse;
import com.erp.finance_servic.service.invoice.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-gateways")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;

    @PostMapping
    public ResponseEntity<PaymentGatewayResponse> createPaymentGateway(@RequestBody PaymentGatewayRequest request) {
        PaymentGatewayResponse response = paymentGatewayService.createPaymentGateway(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PaymentGatewayResponse>> getAllPaymentGateways() {
        List<PaymentGatewayResponse> responses = paymentGatewayService.getAllPaymentGateways();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentGateway(@PathVariable Long id) {
        paymentGatewayService.deletePaymentGateway(id);
        return ResponseEntity.noContent().build();
    }
}