package com.erp.lead_service.service;

import com.erp.lead_service.dto.PriorityAssignDto;
import com.erp.lead_service.dto.PriorityDto;

import java.util.List;

public interface PriorityService {
    // Global priorities management
    PriorityDto createGlobal(PriorityDto dto, String auth);
    PriorityDto updateGlobal(Long id, PriorityDto dto, String auth);
    void deleteGlobal(Long id, String auth);
    List<PriorityDto> getAllGlobal(String auth);

    // Deal-specific priorities
    PriorityDto assignToDeal(Long dealId, PriorityAssignDto dto, String auth);
    PriorityDto updateDealPriority(Long dealId, PriorityAssignDto dto, String auth);
    void removeFromDeal(Long dealId, String auth);
    PriorityDto getByDeal(Long dealId, String auth);

    // Existing methods (for backward compatibility)
    List<PriorityDto> listByDeal(Long dealId, String auth);
}
