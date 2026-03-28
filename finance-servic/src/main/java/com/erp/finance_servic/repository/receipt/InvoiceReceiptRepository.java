package com.erp.finance_servic.repository.receipt;

import com.erp.finance_servic.entity.receipt.InvoiceReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceReceiptRepository extends JpaRepository<InvoiceReceipt, Long> {
    boolean existsByInvoiceId(String invoiceId);
    // Change to:
    List<InvoiceReceipt> findByInvoiceId(String invoiceId);
}
