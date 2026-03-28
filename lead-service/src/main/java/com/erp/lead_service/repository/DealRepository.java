package com.erp.lead_service.repository;

import com.erp.lead_service.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {
    List<Deal> findByLeadId(Long leadId);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Deal d WHERE LOWER(d.title) = LOWER(:title) AND d.lead.id = :leadId")
    boolean existsByTitleIgnoreCaseAndLeadId(@Param("title") String title, @Param("leadId") Long leadId);

    // count all deals for a lead
    @Query("SELECT COUNT(d) FROM Deal d WHERE d.lead.id = :leadId")
    Long countByLeadId(@Param("leadId") Long leadId);

    // count deals in WIN stage for a lead (case-insensitive)
    @Query("SELECT COUNT(d) FROM Deal d WHERE d.lead.id = :leadId AND LOWER(d.dealStage) = 'win'")
    Long countWinsByLeadId(@Param("leadId") Long leadId);

    // total deals in system
    @Query("SELECT COUNT(d) FROM Deal d")
    Long countAllDeals();

    // count deals in WIN stage (case-insensitive)
    @Query("SELECT COUNT(d) FROM Deal d WHERE LOWER(d.dealStage) = 'win'")
    Long countAllWins();

}