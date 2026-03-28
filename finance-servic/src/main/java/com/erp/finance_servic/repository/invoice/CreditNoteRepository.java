package com.erp.finance_servic.repository.invoice;

import com.erp.finance_servic.entity.invoice.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
    Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber);

    // CHANGED: return type List<CreditNote> and query by invoice.invoiceNumber
    List<CreditNote> findByInvoiceInvoiceNumber(String invoiceNumber);

    // Keep by invoice id for internal use if needed (optional)
    List<CreditNote> findByInvoiceId(Long invoiceId);

    boolean existsByCreditNoteNumber(String creditNoteNumber);
    List<CreditNote> findByInvoiceClientId(String clientId);

    List<CreditNote> findByClientId(String clientId);
}
