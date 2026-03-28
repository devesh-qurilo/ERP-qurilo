package com.erp.finance_servic.service.invoice;

import com.erp.finance_servic.dto.invoice.request.CreditNoteCreateRequest;
import com.erp.finance_servic.dto.invoice.request.CreditNoteUpdateRequest;
import com.erp.finance_servic.dto.invoice.response.CreditNoteResponse;
import com.erp.finance_servic.dto.storage.FileMetaDto;
import com.erp.finance_servic.entity.invoice.CreditNote;
import com.erp.finance_servic.entity.invoice.Invoice;
import com.erp.finance_servic.entity.invoice.InvoiceStatus;
import com.erp.finance_servic.repository.invoice.CreditNoteRepository;
import com.erp.finance_servic.repository.invoice.InvoiceRepository;
import com.erp.finance_servic.service.external.ExternalServiceClient;
import com.erp.finance_servic.service.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExternalServiceClient externalServiceClient;
    private final InvoiceService invoiceService; // used for recalc if needed
    private final SupabaseStorageService storageService;

    /**
     * Create credit note for a given invoiceNumber.
     * This saves clientId on creditNote (from invoice) so future "by clientId" queries work.
     */
    @Transactional
    public CreditNoteResponse createCreditNoteForInvoice(String invoiceNumber, CreditNoteCreateRequest request, MultipartFile file) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber));

        // (Optional) business rule: restrict creation to PAID invoices; remove if not required
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new IllegalStateException("Credit note can only be created for PAID invoices");
        }

        if (request.getAmount() == null) {
            throw new IllegalArgumentException("Credit note 'amount' must be provided by frontend");
        }

        CreditNote creditNote = CreditNote.builder()
                .creditNoteNumber(request.getCreditNoteNumber())
                .creditNoteDate(request.getCreditNoteDate())
                .invoice(invoice)
                .currency(request.getCurrency())
                .adjustment(request.getAdjustment())
                .adjustmentPositive(request.getAdjustmentPositive())
                .tax(request.getTax())
                .amount(request.getAmount())
                .notes(request.getNotes())
                .build();

        // Persist clientId from invoice into creditNote for later querying by clientId
        creditNote.setClientId(invoice.getClientId());

        creditNote = creditNoteRepository.save(creditNote);

        // Upload file if provided and attach URL
        if (file != null && !file.isEmpty()) {
            String folder = "credit-notes/" + creditNote.getId();
            FileMetaDto meta = storageService.uploadFile(file, folder, "finance-service");
            if (meta != null) {
                creditNote.setFileUrl(meta.getUrl());
                creditNoteRepository.save(creditNote);
            }
        }

        // Adjustment handling: compute adjustment including tax if any, set signed adjustment on invoice
        BigDecimal adjustmentPlain = request.getAdjustment() == null ? BigDecimal.ZERO : request.getAdjustment();
        BigDecimal taxPercent = request.getTax() == null ? BigDecimal.ZERO : request.getTax();
        BigDecimal adjustmentWithTax = adjustmentPlain.add(adjustmentPlain.multiply(taxPercent).divide(BigDecimal.valueOf(100)));
        BigDecimal signedAdj = Boolean.FALSE.equals(request.getAdjustmentPositive()) ? adjustmentWithTax.negate() : adjustmentWithTax;

        // Replace invoice adjustment with this CN adjustment (business choice shown earlier)
        invoice.setAdjustment(signedAdj);

        // Set invoice.total as frontend provided final amount (so invoice reflects CN total)
        invoice.setTotal(request.getAmount());

        // Recompute unpaid & update status to CREDIT_NOTES
        BigDecimal paid = invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount();
        BigDecimal unpaid = invoice.getTotal().subtract(paid);
        if (unpaid.compareTo(BigDecimal.ZERO) < 0) unpaid = BigDecimal.ZERO;
        invoice.setUnpaidAmount(unpaid);
        invoice.setStatus(InvoiceStatus.CREDIT_NOTES);

        invoiceRepository.save(invoice);

        return mapToCreditNoteResponse(creditNote);
    }

    /**
     * Get credit notes by invoice number.
     */
    public List<CreditNoteResponse> getCreditNotesByInvoiceNumber(String invoiceNumber) {
        return creditNoteRepository.findByInvoiceInvoiceNumber(invoiceNumber)
                .stream()
                .map(this::mapToCreditNoteResponse)
                .collect(Collectors.toList());
    }

    /**
     * Upload/replace file for a credit note.
     */
    @Transactional
    public String uploadCreditNoteFile(Long creditNoteId, MultipartFile file) {
        CreditNote creditNote = creditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new RuntimeException("Credit note not found"));

        // delete previous if exists
        if (creditNote.getFileUrl() != null && !creditNote.getFileUrl().isBlank()) {
            try {
                String prevKey = extractPathFromPublicUrl(creditNote.getFileUrl());
                if (prevKey != null) storageService.deleteFile(prevKey);
            } catch (Exception ignored) {}
        }

        String folder = "credit-notes/" + creditNoteId;
        FileMetaDto meta = storageService.uploadFile(file, folder, "finance-service");
        String publicUrl = meta != null ? meta.getUrl() : null;

        if (publicUrl != null) {
            creditNote.setFileUrl(publicUrl);
            creditNoteRepository.save(creditNote);
        }
        return publicUrl;
    }

    /**
     * Update credit note. Preserves/sets clientId from invoice if missing.
     */
    @Transactional
    public CreditNoteResponse updateCreditNote(Long creditNoteId, CreditNoteUpdateRequest request) {
        CreditNote creditNote = creditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new RuntimeException("Credit note not found"));

        // keep old signed adj in case you want to apply diff logic (not used here)
        BigDecimal oldSignedAdj = signedAdjustmentValue(creditNote.getAdjustment(), creditNote.getAdjustmentPositive(), creditNote.getTax());

        if (request.getCreditNoteDate() != null) creditNote.setCreditNoteDate(request.getCreditNoteDate());
        if (request.getCurrency() != null) creditNote.setCurrency(request.getCurrency());
        if (request.getAdjustment() != null) creditNote.setAdjustment(request.getAdjustment());
        if (request.getAdjustmentPositive() != null) creditNote.setAdjustmentPositive(request.getAdjustmentPositive());
        if (request.getTax() != null) creditNote.setTax(request.getTax());
        if (request.getAmount() != null) creditNote.setAmount(request.getAmount());
        if (request.getNotes() != null) creditNote.setNotes(request.getNotes());

        // Ensure clientId exists on CN (populate from linked invoice)
        if (creditNote.getClientId() == null && creditNote.getInvoice() != null) {
            creditNote.setClientId(creditNote.getInvoice().getClientId());
        }

        creditNote = creditNoteRepository.save(creditNote);

        // Recompute new signed adj using updated fields
        BigDecimal newSignedAdj = signedAdjustmentValue(creditNote.getAdjustment(), creditNote.getAdjustmentPositive(), creditNote.getTax());

        // Apply to invoice: replace previous adjustment with new one
        Invoice invoice = creditNote.getInvoice();
        invoice.setAdjustment(newSignedAdj);

        // Set invoice.total to the creditNote.amount (frontend-provided)
        if (creditNote.getAmount() != null) {
            invoice.setTotal(creditNote.getAmount());
        }

        // Recompute unpaid & status
        BigDecimal paid = invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount();
        BigDecimal unpaid = invoice.getTotal().subtract(paid);
        if (unpaid.compareTo(BigDecimal.ZERO) < 0) unpaid = BigDecimal.ZERO;
        invoice.setUnpaidAmount(unpaid);
        invoice.setStatus(InvoiceStatus.CREDIT_NOTES);

        invoiceRepository.save(invoice);

        return mapToCreditNoteResponse(creditNote);
    }

    /**
     * Delete credit note: remove file, revert invoice adjustment (subtract this CN's signed adj) and update status/unpaid accordingly.
     */
    @Transactional
    public void deleteCreditNote(Long creditNoteId) {
        CreditNote creditNote = creditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new RuntimeException("Credit note not found"));

        // delete attached file if present
        if (creditNote.getFileUrl() != null && !creditNote.getFileUrl().isBlank()) {
            try {
                String key = extractPathFromPublicUrl(creditNote.getFileUrl());
                if (key != null) storageService.deleteFile(key);
            } catch (Exception ignored) {}
        }

        Invoice invoice = creditNote.getInvoice();

        // When deleting, revert invoice.adjustment by subtracting this CN's signed adjustment
        BigDecimal signedAdj = signedAdjustmentValue(creditNote.getAdjustment(), creditNote.getAdjustmentPositive(), creditNote.getTax());
        BigDecimal existingAdj = invoice.getAdjustment() == null ? BigDecimal.ZERO : invoice.getAdjustment();
        invoice.setAdjustment(existingAdj.subtract(signedAdj));

        // Recompute unpaid & status
        BigDecimal paid = invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount();
        BigDecimal unpaid = invoice.getTotal() == null ? BigDecimal.ZERO : invoice.getTotal().subtract(paid);
        if (unpaid.compareTo(BigDecimal.ZERO) < 0) unpaid = BigDecimal.ZERO;
        invoice.setUnpaidAmount(unpaid);

        // If no other credit notes remain, revert status depending on unpaid, else keep CREDIT_NOTES
        boolean otherCN = creditNoteRepository.findByInvoiceInvoiceNumber(invoice.getInvoiceNumber())
                .stream()
                .anyMatch(cn -> !cn.getId().equals(creditNoteId));

        if (!otherCN) {
            if (invoice.getUnpaidAmount() != null && invoice.getUnpaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                invoice.setStatus(InvoiceStatus.PAID);
            } else {
                invoice.setStatus(InvoiceStatus.UNPAID);
            }
        } else {
            invoice.setStatus(InvoiceStatus.CREDIT_NOTES);
        }

        invoiceRepository.save(invoice);
        creditNoteRepository.delete(creditNote);
    }

    /* ---------- helpers & mapping ---------- */

    private CreditNoteResponse mapToCreditNoteResponse(CreditNote creditNote) {
        // Prefer clientId saved on creditNote, fallback to invoice.clientId
        String clientIdToUse = creditNote.getClientId();
        if (clientIdToUse == null && creditNote.getInvoice() != null) {
            clientIdToUse = creditNote.getInvoice().getClientId();
        }

        com.erp.finance_servic.dto.invoice.response.ClientResponse client = null;
        try {
            if (clientIdToUse != null) {
                client = externalServiceClient.getClientById(clientIdToUse);
            }
        } catch (Exception ex) {
            // don't fail mapping if client fetch fails; log and continue with null client
            System.err.println("Failed to fetch client for clientId=" + clientIdToUse + " -> " + ex.getMessage());
            client = null;
        }

        com.erp.finance_servic.dto.invoice.response.ProjectResponse project = null;
        try {
            if (creditNote.getInvoice() != null && creditNote.getInvoice().getProjectId() != null) {
                project = externalServiceClient.getProjectById(creditNote.getInvoice().getProjectId());
            }
        } catch (Exception ex) {
            System.err.println("Failed to fetch project for creditNote id=" + creditNote.getId() + " -> " + ex.getMessage());
            project = null;
        }

        return CreditNoteResponse.builder()
                .id(creditNote.getId())
                .creditNoteNumber(creditNote.getCreditNoteNumber())
                .creditNoteDate(creditNote.getCreditNoteDate())
                .invoiceNumber(creditNote.getInvoice().getInvoiceNumber())
                .currency(creditNote.getCurrency())
                .adjustment(creditNote.getAdjustment())
                .adjustmentPositive(creditNote.getAdjustmentPositive())
                .tax(creditNote.getTax())
                .amount(creditNote.getAmount())
                .notes(creditNote.getNotes())
                .fileUrl(creditNote.getFileUrl())
                .client(client)
                .project(project)
                .totalAmount(creditNote.getInvoice() != null ? creditNote.getInvoice().getTotal() : null)
                .createdAt(creditNote.getCreatedAt())
                .build();
    }

    private BigDecimal signedAdjustmentValue(BigDecimal adjustment, Boolean positive, BigDecimal taxPercent) {
        BigDecimal adj = adjustment == null ? BigDecimal.ZERO : adjustment;
        BigDecimal tax = taxPercent == null ? BigDecimal.ZERO : taxPercent;
        BigDecimal adjWithTax = adj.add(adj.multiply(tax).divide(BigDecimal.valueOf(100)));
        if (Boolean.FALSE.equals(positive)) return adjWithTax.negate();
        return adjWithTax;
    }

    // overload for legacy calls
    private BigDecimal signedAdjustmentValue(BigDecimal adjustment, Boolean positive) {
        return signedAdjustmentValue(adjustment, positive, BigDecimal.ZERO);
    }

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
            return java.net.URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            return null;
        }
    }

    public List<CreditNoteResponse> getAll() {
        return creditNoteRepository.findAll().stream().map(this::mapToCreditNoteResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CreditNoteResponse> getCreditNotesByClientId(String clientId) {
        // Primary: find by credit_note.client_id (new column). Fallback: search by invoice.client_id if needed.
        List<CreditNote> byClientId = creditNoteRepository.findByClientId(clientId);
        if (byClientId != null && !byClientId.isEmpty()) {
            return byClientId.stream().map(this::mapToCreditNoteResponse).collect(Collectors.toList());
        }
        // fallback: find by invoice.client_id (if repository has this method)
        return creditNoteRepository.findByInvoiceClientId(clientId)
                .stream()
                .map(this::mapToCreditNoteResponse)
                .collect(Collectors.toList());
    }
}
