package com.erp.lead_service.repository;

import com.erp.lead_service.entity.LeadNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadNoteRepository extends JpaRepository<LeadNote, Long> {
    List<LeadNote> findByLeadIdOrderByCreatedAtDesc(Long leadId);
    void deleteByLeadId(Long leadId);
    boolean findByLeadId(Long leadId);
}