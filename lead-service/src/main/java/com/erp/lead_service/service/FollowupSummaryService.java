package com.erp.lead_service.service;

import com.erp.lead_service.dto.FollowupSummaryDto;
import com.erp.lead_service.entity.FollowupStatus;
import com.erp.lead_service.repository.DealFollowUpRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Service
public class FollowupSummaryService {

    private final DealFollowUpRepository followupRepository;
    // If you want auth check, inject JwtUtil and validate admin in controller or here.

    public FollowupSummaryService(DealFollowUpRepository followupRepository) {
        this.followupRepository = followupRepository;
    }

    public FollowupSummaryDto getGlobalFollowupSummary() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        // statuses to exclude from counts
        List<FollowupStatus> excluded = Arrays.asList(FollowupStatus.COMPLETED, FollowupStatus.CANCELLED);

        long pending = followupRepository.countByStatusNotInAndNextDateLessThanEqual(excluded, today);
        long upcoming = followupRepository.countByStatusNotInAndNextDateGreaterThan(excluded, today);

        return new FollowupSummaryDto(pending, upcoming);
    }
}
