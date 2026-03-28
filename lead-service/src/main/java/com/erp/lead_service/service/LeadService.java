package com.erp.lead_service.service;

import com.erp.lead_service.dto.Import.ImportResult;
import com.erp.lead_service.dto.Import.LeadImport;
import com.erp.lead_service.dto.lead.LeadDealStatsDto;
import com.erp.lead_service.dto.lead.LeadRequestDto;
import com.erp.lead_service.dto.lead.LeadResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LeadService {
    LeadResponseDto createLead(LeadRequestDto dto, String authHeader);
    LeadResponseDto getLeadById(Long id, String authHeader);
    List<LeadResponseDto> getAllLeads(String authHeader);
    List<LeadResponseDto> getMyLeads(String authHeader);
    LeadResponseDto updateLead(Long id, LeadRequestDto dto, String authHeader);
    void deleteLead(Long id, String authHeader);

    List<ImportResult> importLeadsFromCsv(MultipartFile file, String authHeader);

    LeadDealStatsDto getLeadDealStats(Long leadId, String authHeader);


}
