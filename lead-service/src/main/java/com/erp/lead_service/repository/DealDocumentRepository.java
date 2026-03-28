package com.erp.lead_service.repository;

import com.erp.lead_service.entity.DealDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealDocumentRepository extends JpaRepository<DealDocument, Long> {
    List<DealDocument> findByDealId(Long dealId);

    void deleteByDealId(Long id);
}
