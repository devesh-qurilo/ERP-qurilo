package com.erp.lead_service.repository;

import com.erp.lead_service.entity.DealFollowUp;
import com.erp.lead_service.entity.FollowupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DealFollowUpRepository extends JpaRepository<DealFollowUp, Long> {
    List<DealFollowUp> findByDealIdOrderByNextDateAsc(Long dealId);

    // count where status NOT IN (COMPLETED, CANCELLED) and nextDate <= today
    long countByStatusNotInAndNextDateLessThanEqual(Iterable<FollowupStatus> excludedStatuses, LocalDate date);

    // count where status NOT IN (COMPLETED, CANCELLED) and nextDate > today
    long countByStatusNotInAndNextDateGreaterThan(Iterable<FollowupStatus> excludedStatuses, LocalDate date);

    void deleteByDealId(Long id);
}
