package com.erp.finance_servic.service.invoice;

import com.erp.finance_servic.dto.invoice.request.PaymentCreateRequest;
import com.erp.finance_servic.dto.invoice.request.PaymentUpdateRequest;
import com.erp.finance_servic.dto.invoice.response.*;
import com.erp.finance_servic.dto.storage.FileMetaDto;
import com.erp.finance_servic.entity.invoice.Invoice;
import com.erp.finance_servic.entity.invoice.Payment;
import com.erp.finance_servic.entity.invoice.PaymentStatus;
import com.erp.finance_servic.repository.invoice.InvoiceRepository;
import com.erp.finance_servic.repository.invoice.PaymentGatewayRepository;
import com.erp.finance_servic.repository.invoice.PaymentRepository;
import com.erp.finance_servic.service.external.ExternalServiceClient;
import com.erp.finance_servic.service.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGatewayRepository paymentGatewayRepository;
    private final InvoiceService invoiceService; // to recalc invoice totals
    private final SupabaseStorageService storageService;
    private final ExternalServiceClient externalServiceClient;

    /**
     * Create payment with optional receipt file (multipart flow).
     */
    @Transactional
    public PaymentResponse createPaymentWithReceipt(PaymentCreateRequest request, MultipartFile file) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(request.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // ensure latest invoice totals
        invoice = invoiceService.recalculateInvoice(invoice);

        if (request.getAmount() == null) throw new IllegalArgumentException("amount is required");

        // Validate payment doesn't exceed unpaid amount
        if (request.getAmount().compareTo(invoice.getUnpaidAmount()) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed unpaid amount");
        }

        var paymentGateway = paymentGatewayRepository.findById(request.getPaymentGatewayId())
                .orElseThrow(() -> new RuntimeException("Payment gateway not found"));

        Payment payment = Payment.builder()
                .invoice(invoice)
                .projectId(request.getProjectId())
                .clientId(request.getClientId())
                .currency(request.getCurrency())
                .amount(request.getAmount())
                .transactionId(request.getTransactionId())
                .paymentGateway(paymentGateway)
                .status(PaymentStatus.COMPLETED)
                .note(request.getNotes())
                .build();

        payment = paymentRepository.save(payment);

        // Handle receipt file upload if present
        if (file != null && !file.isEmpty()) {
            String folder = "payments/" + payment.getId();
            FileMetaDto meta = storageService.uploadFile(file, folder, "finance-service");
            if (meta != null) {
                payment.setReceiptFileUrl(meta.getUrl());
                paymentRepository.save(payment);
            }
        }

        // Update invoice paid amount
        BigDecimal existingPaid = invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount();
        invoice.setPaidAmount(existingPaid.add(request.getAmount()));

        // Recalculate invoice totals & status
        invoice = invoiceService.recalculateInvoice(invoice);
        invoiceRepository.save(invoice);

        return mapToPaymentResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByInvoiceNumber(String invoiceNumber) {
        return paymentRepository.findByInvoiceInvoiceNumber(invoiceNumber).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByClientId(String clientId) {
        return paymentRepository.findByClientId(clientId).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByProjectId(String projectId) {
        return paymentRepository.findByProjectId(projectId).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public String uploadPaymentReceipt(Long paymentId, MultipartFile file) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // If existing receipt exists, delete it first
        if (payment.getReceiptFileUrl() != null && !payment.getReceiptFileUrl().isBlank()) {
            try {
                String existingKey = extractPathFromPublicUrl(payment.getReceiptFileUrl());
                if (existingKey != null) storageService.deleteFile(existingKey);
            } catch (Exception ex) {
                System.err.println("Failed to delete existing receipt before upload: " + ex.getMessage());
            }
        }

        String folder = "payments/" + paymentId;
        FileMetaDto meta = storageService.uploadFile(file, folder, "finance-service");
        String publicUrl = meta != null ? meta.getUrl() : null;

        if (publicUrl != null) {
            payment.setReceiptFileUrl(publicUrl);
            paymentRepository.save(payment);
        }
        return publicUrl;
    }

    @Transactional
    public PaymentResponse updatePayment(Long paymentId, PaymentUpdateRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        BigDecimal oldAmount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();

        if (request.getAmount() != null) payment.setAmount(request.getAmount());
        if (request.getCurrency() != null) payment.setCurrency(request.getCurrency());
        if (request.getTransactionId() != null) payment.setTransactionId(request.getTransactionId());
        if (request.getPaymentGatewayId() != null) {
            var pg = paymentGatewayRepository.findById(request.getPaymentGatewayId())
                    .orElseThrow(() -> new RuntimeException("Payment gateway not found"));
            payment.setPaymentGateway(pg);
        }
        if (request.getStatus() != null) {
            try {
                payment.setStatus(PaymentStatus.valueOf(request.getStatus()));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid payment status");
            }
        }
        if (request.getNotes() != null) payment.setNote(request.getNotes());

        payment = paymentRepository.save(payment);

        // If amount changed, adjust invoice.paidAmount
        if (request.getAmount() != null) {
            Invoice invoice = payment.getInvoice();
            BigDecimal paid = invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount();
            BigDecimal newPaid = paid.subtract(oldAmount).add(request.getAmount());
            if (newPaid.compareTo(BigDecimal.ZERO) < 0) newPaid = BigDecimal.ZERO;
            invoice.setPaidAmount(newPaid);
            invoice = invoiceService.recalculateInvoice(invoice);
            invoiceRepository.save(invoice);
        }

        return mapToPaymentResponse(payment);
    }

    @Transactional
    public void deletePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // delete receipt file if present
        if (payment.getReceiptFileUrl() != null && !payment.getReceiptFileUrl().isBlank()) {
            try {
                String key = extractPathFromPublicUrl(payment.getReceiptFileUrl());
                if (key != null) storageService.deleteFile(key);
            } catch (Exception ex) {
                System.err.println("Failed to delete payment receipt file: " + ex.getMessage());
            }
        }

        // adjust invoice paid amount
        Invoice invoice = payment.getInvoice();
        BigDecimal paid = invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount();
        BigDecimal toSubtract = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        BigDecimal newPaid = paid.subtract(toSubtract);
        if (newPaid.compareTo(BigDecimal.ZERO) < 0) newPaid = BigDecimal.ZERO;
        invoice.setPaidAmount(newPaid);

        invoice = invoiceService.recalculateInvoice(invoice);
        invoiceRepository.save(invoice);

        paymentRepository.delete(payment);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        ClientResponse client = null;
        ProjectResponse project = null;
        try {
            client = externalServiceClient.getClientById(payment.getClientId());
        } catch (Exception ignored) {}
        try {
            project = externalServiceClient.getProjectById(payment.getProjectId());
        } catch (Exception ignored) {}

        return PaymentResponse.builder()
                .id(payment.getId())
                .projectId(payment.getProjectId())
                .project(project)
                .clientId(payment.getClientId())
                .client(client)
                .currency(payment.getCurrency())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .paymentGateway(payment.getPaymentGateway() != null ?
                        PaymentGatewayResponse.builder()
                                .id(payment.getPaymentGateway().getId())
                                .name(payment.getPaymentGateway().getName())
                                .createdAt(payment.getPaymentGateway().getCreatedAt())
                                .build() : null)
                .receiptFileUrl(payment.getReceiptFileUrl())
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .note(payment.getNote())
                .paymentDate(payment.getPaymentDate())
                .invoice(InvoiceSimpleResponse.builder()
                        .id(payment.getInvoice().getId())
                        .invoiceNumber(payment.getInvoice().getInvoiceNumber())
                        .total(payment.getInvoice().getTotal())
                        .status(payment.getInvoice().getStatus() != null ? payment.getInvoice().getStatus().name() : null)
                        .build())
                .build();
    }

    /**
     * Extract internal object path from Supabase public URL:
     */
    private String extractPathFromPublicUrl(String publicUrl) {
        try {
            if (publicUrl == null || publicUrl.isBlank()) return null;
            String marker = "/storage/v1/object/public/";
            int idx = publicUrl.indexOf(marker);
            if (idx == -1) return null;
            String after = publicUrl.substring(idx + marker.length());
            int slash = after.indexOf('/');
            if (slash == -1) return null;
            String encodedPath = after.substring(slash + 1); // skip bucket
            return URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            return null;
        }
    }
}
