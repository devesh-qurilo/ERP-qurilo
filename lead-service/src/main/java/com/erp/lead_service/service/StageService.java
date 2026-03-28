package com.erp.lead_service.service;

import com.erp.lead_service.dto.stage.StageRequestDto;
import com.erp.lead_service.dto.stage.StageResponseDto;

import java.util.List;

public interface StageService {
    StageResponseDto createStage(StageRequestDto dto, String authHeader);
    StageResponseDto updateStage(Long id, StageRequestDto dto, String authHeader);
    void deleteStage(Long id, String authHeader);
    List<StageResponseDto> listStages();
    StageResponseDto getStage(Long id);
}
