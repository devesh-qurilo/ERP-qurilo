package com.erp.lead_service.repository;

import com.erp.lead_service.entity.DealNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealNoteRepository extends JpaRepository<DealNote, Long> {
    List<DealNote> findByDealIdOrderByCreatedAtDesc(Long dealId);
    void deleteByDealId(Long id);
}
