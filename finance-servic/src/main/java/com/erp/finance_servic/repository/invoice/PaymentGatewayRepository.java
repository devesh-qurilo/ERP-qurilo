package com.erp.finance_servic.repository.invoice;

import com.erp.finance_servic.entity.invoice.PaymentGatewayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentGatewayRepository extends JpaRepository<PaymentGatewayEntity, Long> {
    Optional<PaymentGatewayEntity> findByName(String name);
    boolean existsByName(String name);
}
