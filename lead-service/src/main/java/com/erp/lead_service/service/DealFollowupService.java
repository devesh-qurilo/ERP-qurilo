package com.erp.lead_service.service;

import com.erp.lead_service.dto.FollowupRequestDto;
import com.erp.lead_service.dto.FollowupResponseDto;
import com.erp.lead_service.dto.FollowupUpdateRequestDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DealFollowupService {
    FollowupResponseDto addFollowup(Long dealId, FollowupRequestDto dto, String authHeader);
    List<FollowupResponseDto> listFollowups(Long dealId, String authHeader);
    void deleteFollowup(Long dealId, Long followupId, String authHeader);

    // NEW
    @Transactional
    FollowupResponseDto updateFollowup(Long dealId, Long followupId, FollowupUpdateRequestDto dto, String authHeader);

    FollowupResponseDto getFollowup(Long dealId, Long followupId, String authHeader);
}
