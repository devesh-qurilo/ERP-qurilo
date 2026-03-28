package com.erp.lead_service.service;

import com.erp.lead_service.dto.TagRequestDto;
import com.erp.lead_service.dto.TagResponseDTO;
import com.erp.lead_service.entity.DealTag;

import java.util.List;

public interface DealTagService {
    List<TagResponseDTO> getTagsForDeal(Long dealId, String auth);
    void addTag(Long dealId, TagRequestDto dto, String authHeader);
    void removeTag(Long dealId, Long tagId, String authHeader);
}
