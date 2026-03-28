package com.erp.finance_servic.repository.invoice;
import com.erp.finance_servic.entity.invoice.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoiceId(Long invoiceId);
    List<Payment> findByClientId(String clientId);
    List<Payment> findByProjectId(String projectId);
    List<Payment> findByPaymentGatewayId(Long paymentGatewayId);

    // ✅ FIXED: proper return type
    List<Payment> findByInvoiceInvoiceNumber(String invoiceNumber);
}
