package com.erp.lead_service.service;

import com.erp.lead_service.dto.Import.ImportResult;
import com.erp.lead_service.dto.deal.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DealService {

    @Transactional
    DealResponseDto createDeal(DealRequestDto dto, String authHeader);

    DealResponseDto getDealById(Long id, String authHeader);

    List<DealResponseDto> getAllDeals(String authHeader);

    @Transactional
    DealResponseDto updateDealStage(Long dealId, String newStage, String authHeader);

    // ------------------ Full Deal Update ------------------
    @Transactional
    DealResponseDto updateDeal(Long dealId, DealRequestDto dto, String authHeader);

    List<DealResponseDto> getDealByLeadId(Long id, String auth);

    List<ImportResult> importDealsFromCsv(MultipartFile file, String authHeader);

    DealStatsDto getGlobalDealStats(String authHeader);


    void deleteDeal(Long id, String authHeader);
}