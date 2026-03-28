package com.erp.lead_service.mapper;

import com.erp.lead_service.dto.deal.DealRequestDto;
import com.erp.lead_service.dto.deal.DealResponseDto;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.Lead;
import org.springframework.stereotype.Component;

@Component
public class DealMapper {

    public Deal toEntity(DealRequestDto dto, Lead lead) {
        Deal deal = new Deal();
        deal.setTitle(dto.getTitle());
        deal.setValue(dto.getValue());
        deal.setCurrency(dto.getCurrency());
        deal.setDealStage(dto.getDealStage());
        deal.setDealAgent(dto.getDealAgent());
        deal.setDealWatchers(dto.getDealWatchers() != null ? dto.getDealWatchers() : java.util.Collections.emptyList());
        deal.setLead(lead);
        deal.setExpectedCloseDate(dto.getExpectedCloseDate());
        deal.setPipeline(dto.getPipeline());
        deal.setDealCategory(dto.getDealCategory());
        return deal;
    }

    public DealResponseDto toDto(Deal deal) {
        DealResponseDto dto = new DealResponseDto();
        dto.setId(deal.getId());
        dto.setTitle(deal.getTitle());
        dto.setValue(deal.getValue());
        dto.setCurrency(deal.getCurrency());
        dto.setDealStage(deal.getDealStage());
        dto.setDealAgent(deal.getDealAgent());
        dto.setDealWatchers(deal.getDealWatchers());
        dto.setLeadId(deal.getLead() != null ? deal.getLead().getId() : null);
        // NEW: set leadName and leadMobile if lead present
        if (deal.getLead() != null) {
            dto.setLeadName(deal.getLead().getName());
            // adjust field name if your Lead entity uses different getter (e.g., getMobileNumber)
            dto.setLeadMobile(deal.getLead().getMobileNumber());
            dto.setLeadEmail(deal.getLead().getEmail());
            dto.setLeadCompany(deal.getLead().getCompanyName());
        }
        dto.setPipeline(deal.getPipeline());
        dto.setDealCategory(deal.getDealCategory());
        dto.setExpectedCloseDate(deal.getExpectedCloseDate());
        dto.setCreatedAt(deal.getCreatedAt());
        dto.setUpdatedAt(deal.getUpdatedAt());
        return dto;
    }
}
