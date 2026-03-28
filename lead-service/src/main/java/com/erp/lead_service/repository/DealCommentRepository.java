package com.erp.lead_service.repository;

import com.erp.lead_service.entity.DealComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealCommentRepository extends JpaRepository<DealComment, Long> {
    List<DealComment> findByDealIdOrderByCreatedAtDesc(Long dealId);

    void deleteByDealId(Long id);
}
