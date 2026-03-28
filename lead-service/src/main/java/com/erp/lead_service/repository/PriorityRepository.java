package com.erp.lead_service.repository;

import com.erp.lead_service.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PriorityRepository extends JpaRepository<Priority, Long> {
    List<Priority> findByDealId(Long dealId);

    List<Priority> findByIsGlobalTrue();

    // Corrected query - Priority entity check karega
    @Query("SELECT COUNT(p) FROM Priority p WHERE p.deal IS NOT NULL AND p.id = :priorityId")
    long countByAssignedPriority(@Param("priorityId") Long priorityId);

    void deleteByDealId(Long id);
}
