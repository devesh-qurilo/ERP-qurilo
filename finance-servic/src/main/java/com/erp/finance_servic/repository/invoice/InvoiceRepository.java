package com.erp.finance_servic.repository.invoice;

import com.erp.finance_servic.entity.invoice.Invoice;
import com.erp.finance_servic.entity.invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    boolean existsByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByClientId(String clientId);

    List<Invoice> findByProjectId(String projectId);

    @Modifying
    @Query("UPDATE Invoice i SET i.status = :status, i.updatedAt = CURRENT_TIMESTAMP WHERE i.invoiceNumber = :invoiceNumber")
    int updateStatusByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber,
                                    @Param("status") InvoiceStatus status);
}
