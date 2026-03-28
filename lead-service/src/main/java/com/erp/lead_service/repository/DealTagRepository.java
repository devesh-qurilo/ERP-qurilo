package com.erp.lead_service.repository;

import com.erp.lead_service.entity.DealTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealTagRepository extends JpaRepository<DealTag, Long> {
    List<DealTag> findByDealId(Long dealId);

    void deleteByDealId(Long id);
}
