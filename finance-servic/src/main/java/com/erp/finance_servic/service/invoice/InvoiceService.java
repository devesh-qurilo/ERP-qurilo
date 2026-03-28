package com.erp.finance_servic.service.invoice;

import com.erp.finance_servic.dto.invoice.request.InvoiceCreateRequest;
import com.erp.finance_servic.dto.invoice.request.InvoiceUpdateRequest;
import com.erp.finance_servic.dto.invoice.response.InvoiceResponse;
import com.erp.finance_servic.dto.invoice.response.UnpaidInvoiceStatsDto;
import com.erp.finance_servic.dto.storage.FileMetaDto;
import com.erp.finance_servic.entity.invoice.Invoice;
import com.erp.finance_servic.entity.invoice.InvoiceStatus;
import com.erp.finance_servic.repository.invoice.InvoiceRepository;
import com.erp.finance_servic.service.external.ExternalServiceClient;
import com.erp.finance_servic.service.notification.EmailService;
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
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ExternalServiceClient externalServiceClient;
    private final SupabaseStorageService storageService;
    private final EmailService emailService; // 🔽 add

    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request) {
        // Validate discount
        if (request.getDiscount() != null &&
                (request.getDiscount().compareTo(java.math.BigDecimal.valueOf(100)) > 0 ||
                        request.getDiscount().compareTo(java.math.BigDecimal.ZERO) < 0)) {
            throw new IllegalArgumentException("Discount must be between 0 and 100");
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(request.getInvoiceNumber())
                .invoiceDate(request.getInvoiceDate())
                .currency(request.getCurrency())
                .projectId(request.getProjectId())
                .clientId(request.getClientId())
                .amount(request.getAmount())
                .tax(request.getTax())
                .discount(request.getDiscount())
                .amountInWords(request.getAmountInWords())
                .notes(request.getNotes())
                .status(InvoiceStatus.UNPAID)
                .paidAmount(java.math.BigDecimal.ZERO)
                .adjustment(java.math.BigDecimal.ZERO)
                .build();

        // Fetch project budget from project service
        var project = externalServiceClient.getProjectById(request.getProjectId());
        invoice.setProjectBudget(project.getBudget());

        // Recalculate totals before save
        invoice = recalculateInvoice(invoice);

        invoice = invoiceRepository.save(invoice);
        return mapToInvoiceResponse(invoice);
    }

    public InvoiceResponse getInvoiceById(String id) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        // ensure latest calculations
        invoice = recalculateInvoice(invoice);
        invoiceRepository.save(invoice);
        return mapToInvoiceResponse(invoice);
    }

    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Recalculate invoice totals, unpaid and status.
     * This method is idempotent and null-safe.
     */
    /*
    @Transactional
    public Invoice recalculateInvoice(Invoice invoice) {
        if (invoice.getAmount() == null) invoice.setAmount(BigDecimal.ZERO);
        if (invoice.getDiscount() == null) invoice.setDiscount(BigDecimal.ZERO);
        if (invoice.getTax() == null) invoice.setTax(BigDecimal.ZERO);
        if (invoice.getAdjustment() == null) invoice.setAdjustment(BigDecimal.ZERO);
        if (invoice.getPaidAmount() == null) invoice.setPaidAmount(BigDecimal.ZERO);

        // discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (invoice.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = invoice.getAmount().multiply(invoice.getDiscount()).divide(BigDecimal.valueOf(100));
        }

        BigDecimal taxable = invoice.getAmount().subtract(discountAmount);

        BigDecimal taxAmount = BigDecimal.ZERO;
        if (invoice.getTax().compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = taxable.multiply(invoice.getTax()).divide(BigDecimal.valueOf(100));
        }

        BigDecimal total = taxable.add(taxAmount).add(invoice.getAdjustment() == null ? BigDecimal.ZERO : invoice.getAdjustment());
        invoice.setTotal(total.setScale(2, java.math.RoundingMode.HALF_EVEN));

        BigDecimal unpaid = invoice.getTotal().subtract(invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount());
        if (unpaid.compareTo(BigDecimal.ZERO) < 0) unpaid = BigDecimal.ZERO;
        invoice.setUnpaidAmount(unpaid.setScale(2, java.math.RoundingMode.HALF_EVEN));

        if (invoice.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (invoice.getStatus() != InvoiceStatus.CREDIT_NOTES) {
            invoice.setStatus(InvoiceStatus.UNPAID);
        }

        return invoice;
    }

     */

    @Transactional
    public Invoice recalculateInvoice(Invoice invoice) {
        if (invoice.getAmount() == null) invoice.setAmount(BigDecimal.ZERO);
        if (invoice.getDiscount() == null) invoice.setDiscount(BigDecimal.ZERO);
        if (invoice.getTax() == null) invoice.setTax(BigDecimal.ZERO);
        if (invoice.getAdjustment() == null) invoice.setAdjustment(BigDecimal.ZERO);
        if (invoice.getPaidAmount() == null) invoice.setPaidAmount(BigDecimal.ZERO);

        // discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (invoice.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = invoice.getAmount().multiply(invoice.getDiscount()).divide(BigDecimal.valueOf(100));
        }

        BigDecimal taxable = invoice.getAmount().subtract(discountAmount);

        BigDecimal taxAmount = BigDecimal.ZERO;
        if (invoice.getTax().compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = taxable.multiply(invoice.getTax()).divide(BigDecimal.valueOf(100));
        }

        BigDecimal total = taxable.add(taxAmount).add(invoice.getAdjustment() == null ? BigDecimal.ZERO : invoice.getAdjustment());
        invoice.setTotal(total.setScale(2, java.math.RoundingMode.HALF_EVEN));

        BigDecimal unpaid = invoice.getTotal().subtract(invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount());
        if (unpaid.compareTo(BigDecimal.ZERO) < 0) unpaid = BigDecimal.ZERO;
        invoice.setUnpaidAmount(unpaid.setScale(2, java.math.RoundingMode.HALF_EVEN));

        // Preserve CREDIT_NOTES status: if invoice already has CREDIT_NOTES, do not override it.
        if (invoice.getStatus() == InvoiceStatus.CREDIT_NOTES) {
            // keep CREDIT_NOTES regardless of unpaid; nothing to change
        } else {
            // for non-CREDIT_NOTES statuses, apply existing logic
            if (invoice.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                invoice.setStatus(InvoiceStatus.PAID);
            } else {
                invoice.setStatus(InvoiceStatus.UNPAID);
            }
        }

        return invoice;
    }

//    @Transactional
//    public void markInvoiceAsPaid(String id) {
//        Invoice invoice = invoiceRepository.findByInvoiceNumber(id)
//                .orElseThrow(() -> new RuntimeException("Invoice not found"));
//
//        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
//            throw new IllegalStateException("Only unpaid invoices can be marked as paid");
//        }
//
//        // Ensure amounts are up to date
//        invoice = recalculateInvoice(invoice);
//
//        if (invoice.getUnpaidAmount() == null || invoice.getUnpaidAmount().compareTo(java.math.BigDecimal.ZERO) != 0) {
//            throw new IllegalStateException("Invoice unpaid amount must be zero to mark as paid");
//        }
//
//        invoice.setStatus(InvoiceStatus.PAID);
//        invoiceRepository.save(invoice);
//    }


//    @Transactional
//    public void markInvoiceAsPaid(String id) {
//        Invoice invoice = invoiceRepository.findByInvoiceNumber(id)
//                .orElseThrow(() -> new RuntimeException("Invoice not found"));
//
//        if (invoice.getStatus() != null) invoice.setStatus(InvoiceStatus.PAID);
//        invoiceRepository.save(invoice);
//    }

    @Transactional
    public void markInvoiceAsPaid(String id) {
        int updatedRows = invoiceRepository.updateStatusByInvoiceNumber(id, InvoiceStatus.PAID);

        if (updatedRows == 0) {
            throw new RuntimeException("Invoice not found with number: " + id);
        }
    }

    /*****************
     * File operations
     *****************/

    @Transactional
    public String uploadInvoiceFile(String invoiceId, MultipartFile file) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        String folder = "invoices/" + invoiceId;
        // SupabaseStorageService.uploadFile(file, folder, uploadedBy) returns FileMetaDto
        FileMetaDto meta = storageService.uploadFile(file, folder, "finance-service");
        String publicUrl = meta != null ? meta.getUrl() : null;

        if (publicUrl != null) {
            invoice.getFileUrls().add(publicUrl);
            invoice = recalculateInvoice(invoice);
            invoiceRepository.save(invoice);
        }

        return publicUrl;
    }

    @Transactional
    public void deleteInvoiceFile(String invoiceId, String fileUrl) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        String key = extractPathFromPublicUrl(fileUrl);
        if (key != null) {
            // delete by internal path
            storageService.deleteFile(key);
        }

        invoice.getFileUrls().removeIf(u -> u.equals(fileUrl));
        invoice = recalculateInvoice(invoice);
        invoiceRepository.save(invoice);
    }

    /**
     * Extract internal object path from Supabase public URL:
     * Example public URL:
     * https://<project>.supabase.co/storage/v1/object/public/{bucket}/{objectPath}
     * This returns {objectPath} (URL-decoded).
     */
    private String extractPathFromPublicUrl(String publicUrl) {
        try {
            if (publicUrl == null || publicUrl.isBlank()) return null;
            String marker = "/storage/v1/object/public/";
            int idx = publicUrl.indexOf(marker);
            if (idx == -1) return null;
            String after = publicUrl.substring(idx + marker.length());
            // after = "{bucket}/{encodedPath}"
            int slash = after.indexOf('/');
            if (slash == -1) return null;
            String encodedPath = after.substring(slash + 1); // skip bucket
            // URL-decode
            return URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            // if extraction fails, log and return null (caller will skip delete)
            return null;
        }
    }

//    private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
//        var client = externalServiceClient.getClientById(invoice.getClientId());
//        var project = externalServiceClient.getProjectById(invoice.getProjectId());
//
//        return InvoiceResponse.builder()
//                .id(invoice.getId())
//                .invoiceNumber(invoice.getInvoiceNumber())
//                .invoiceDate(invoice.getInvoiceDate())
//                .currency(invoice.getCurrency())
//                .client(client)
//                .project(project)
//                .projectBudget(invoice.getProjectBudget())
//                .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
//                .amount(invoice.getAmount())
//                .tax(invoice.getTax())
//                .discount(invoice.getDiscount())
//                .total(invoice.getTotal())
//                .amountInWords(invoice.getAmountInWords())
//                .notes(invoice.getNotes())
//                .fileUrls(invoice.getFileUrls())
//                .paidAmount(invoice.getPaidAmount())
//                .unpaidAmount(invoice.getUnpaidAmount())
//                .adjustment(invoice.getAdjustment())
//                .createdAt(invoice.getCreatedAt())
//                .build();
//    }

    private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
        // local logger so this method is safe even if the class has no @Slf4j
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvoiceService.class);

        com.erp.finance_servic.dto.invoice.response.ClientResponse client = null;
        com.erp.finance_servic.dto.invoice.response.ProjectResponse project = null;

        // safe client fetch
        try {
            if (invoice.getClientId() != null && !invoice.getClientId().isBlank()) {
                client = externalServiceClient.getClientById(invoice.getClientId());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch client {} for invoice {}: {}", invoice.getClientId(), invoice.getInvoiceNumber(), e.getMessage());
            // leave client as null (caller/consumer should handle nulls)
        }

        // safe project fetch
        try {
            if (invoice.getProjectId() != null && !invoice.getProjectId().isBlank()) {
                project = externalServiceClient.getProjectById(invoice.getProjectId());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch project {} for invoice {}: {}", invoice.getProjectId(), invoice.getInvoiceNumber(), e.getMessage());
            // leave project as null
        }

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .currency(invoice.getCurrency())
                .client(client)                // may be null if fetch failed
                .project(project)              // may be null if fetch failed
                .projectBudget(invoice.getProjectBudget())
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
                .amount(invoice.getAmount())
                .tax(invoice.getTax())
                .discount(invoice.getDiscount())
                .total(invoice.getTotal())
                .amountInWords(invoice.getAmountInWords())
                .notes(invoice.getNotes())
                .fileUrls(invoice.getFileUrls())
                .paidAmount(invoice.getPaidAmount())
                .unpaidAmount(invoice.getUnpaidAmount())
                .adjustment(invoice.getAdjustment())
                .createdAt(invoice.getCreatedAt())
                .build();
    }


    /**
     * NEW: update invoice (partial update).
     * Only non-null fields from InvoiceUpdateRequest are applied.
     */
    @Transactional
    public InvoiceResponse updateInvoice(String invoiceNumber, InvoiceUpdateRequest request) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (request.getInvoiceDate() != null) invoice.setInvoiceDate(request.getInvoiceDate());
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) invoice.setCurrency(request.getCurrency());
        if (request.getAmount() != null) invoice.setAmount(request.getAmount());
        if (request.getTax() != null) invoice.setTax(request.getTax());
        if (request.getDiscount() != null) {
            if (request.getDiscount().compareTo(BigDecimal.ZERO) < 0 || request.getDiscount().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Discount must be between 0 and 100");
            }
            invoice.setDiscount(request.getDiscount());
        }
        if (request.getAmountInWords() != null) invoice.setAmountInWords(request.getAmountInWords());
        if (request.getNotes() != null) invoice.setNotes(request.getNotes());

        // Recalculate totals + status after modifications
        invoice = recalculateInvoice(invoice);
        invoice = invoiceRepository.save(invoice);

        return mapToInvoiceResponse(invoice);
    }

    /**
     * NEW: delete invoice by invoiceNumber and remove associated files from storage.
     * This will also delete invoice entity (cascade will remove payments/creditNotes if mapped).
     */
    @Transactional
    public void deleteInvoice(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Delete all files from storage (if any)
        if (invoice.getFileUrls() != null) {
            for (String fileUrl : invoice.getFileUrls()) {
                try {
                    String key = extractPathFromPublicUrl(fileUrl);
                    if (key != null) {
                        storageService.deleteFile(key);
                    }
                } catch (Exception ex) {
                    // log and continue deleting other files
                    // You can integrate a logger; using System.err for minimal change
                    System.err.println("Failed to delete file from storage: " + fileUrl + " -> " + ex.getMessage());
                }
            }
        }

        // Optionally delete receipts from payments and credit note files
        // If payments/creditNotes store files externally, delete them too.
        // We'll attempt to delete payment receipts and credit note fileUrls if present
        if (invoice.getPayments() != null) {
            invoice.getPayments().forEach(p -> {
                if (p.getReceiptFileUrl() != null && !p.getReceiptFileUrl().isBlank()) {
                    try {
                        String key = extractPathFromPublicUrl(p.getReceiptFileUrl());
                        if (key != null) storageService.deleteFile(key);
                    } catch (Exception ex) {
                        System.err.println("Failed to delete payment receipt: " + p.getReceiptFileUrl());
                    }
                }
            });
        }
        if (invoice.getCreditNotes() != null) {
            invoice.getCreditNotes().forEach(cn -> {
                if (cn.getFileUrl() != null && !cn.getFileUrl().isBlank()) {
                    try {
                        String key = extractPathFromPublicUrl(cn.getFileUrl());
                        if (key != null) storageService.deleteFile(key);
                    } catch (Exception ex) {
                        System.err.println("Failed to delete credit note file: " + cn.getFileUrl());
                    }
                }
            });
        }

        // Finally delete the invoice entity -> cascade should remove related payments/creditNotes
        invoiceRepository.delete(invoice);
    }


    /**
     * Send payment reminder email to the invoice client iff invoice is UNPAID.
     * Does not modify any invoice amounts/status. Pure side-effect (email).
     */
    @Transactional(readOnly = true)
    public void sendUnpaidReminderEmail(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // ensure latest numbers (read-only effect in our flow)
        recalculateInvoice(invoice);

        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new IllegalStateException("Reminder allowed only for UNPAID invoices");
        }

        // fetch client & project meta for nice email
        var client = externalServiceClient.getClientById(invoice.getClientId());
        var project = externalServiceClient.getProjectById(invoice.getProjectId());

        String to = client != null ? client.getEmail() : null;
        if (to == null || to.isBlank()) {
            throw new IllegalStateException("Client email not available for this invoice");
        }

        String subject = "[Payment Reminder] Invoice " + invoice.getInvoiceNumber();
        String html = """
            <p>Dear %s,</p>
            <p>This is a friendly reminder for payment of invoice <b>%s</b>.</p>
            <ul>
              <li><b>Project:</b> %s</li>
              <li><b>Invoice Date:</b> %s</li>
              <li><b>Total:</b> %s %s</li>
              <li><b>Unpaid:</b> %s %s</li>
            </ul>
            <p>Please ignore if already paid.</p>
            <p>Regards,<br/>Finance Team</p>
        """.formatted(
                client.getName() != null ? client.getName() : "Customer",
                invoice.getInvoiceNumber(),
                project != null ? (project.getProjectName() != null ? project.getProjectName() : "N/A") : "N/A",
                String.valueOf(invoice.getInvoiceDate()),
                invoice.getCurrency() != null ? invoice.getCurrency() : "",
                invoice.getTotal() != null ? invoice.getTotal().toPlainString() : "0.00",
                invoice.getCurrency() != null ? invoice.getCurrency() : "",
                invoice.getUnpaidAmount() != null ? invoice.getUnpaidAmount().toPlainString() : "0.00"
        );

        emailService.send(to, subject, html);
    }

    /**
     * Return all invoices for a given clientId (no pagination).
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByClientId(String clientId) {
        return invoiceRepository.findByClientId(clientId)
                .stream()
                .map(this::mapToInvoiceResponse) // re-use existing mapper to include client/project meta
                .collect(Collectors.toList());
    }

    /**
     * Return all invoices for a given projectId (no pagination).
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByProjectId(String projectId) {
        return invoiceRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UnpaidInvoiceStatsDto getUnpaidInvoiceStatsByClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return UnpaidInvoiceStatsDto.builder()
                    .unpaidInvoiceCount(0L)
                    .totalUnpaidAmount(BigDecimal.ZERO)
                    .build();
        }

        // Load invoices for the client (you already have findByClientId)
        List<Invoice> invoices = invoiceRepository.findByClientId(clientId);

        if (invoices == null || invoices.isEmpty()) {
            return UnpaidInvoiceStatsDto.builder()
                    .unpaidInvoiceCount(0L)
                    .totalUnpaidAmount(BigDecimal.ZERO)
                    .build();
        }

        long count = 0L;
        BigDecimal totalUnpaid = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            try {
                // Ensure totals/unpaid are up-to-date for each invoice
                Invoice recalculated = recalculateInvoice(inv);

                if (recalculated.getStatus() == InvoiceStatus.UNPAID) {
                    count++;
                    BigDecimal u = recalculated.getUnpaidAmount() == null ? BigDecimal.ZERO : recalculated.getUnpaidAmount();
                    totalUnpaid = totalUnpaid.add(u);
                }
            } catch (Exception ex) {
                // Best-effort: skip problematic invoices but continue counting others
                System.err.println("Failed to process invoice for stats: " + inv.getInvoiceNumber() + " -> " + ex.getMessage());
            }
        }

        return UnpaidInvoiceStatsDto.builder()
                .unpaidInvoiceCount(count)
                .totalUnpaidAmount(totalUnpaid.setScale(2, java.math.RoundingMode.HALF_EVEN))
                .build();
    }

}
