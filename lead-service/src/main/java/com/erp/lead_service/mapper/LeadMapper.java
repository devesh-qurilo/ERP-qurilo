package com.erp.lead_service.mapper;

import com.erp.lead_service.dto.lead.LeadRequestDto;
import com.erp.lead_service.dto.lead.LeadResponseDto;
import com.erp.lead_service.entity.Lead;
import com.erp.lead_service.entity.LeadStatus;
import org.springframework.stereotype.Component;

@Component
public class LeadMapper {

    public Lead toEntity(LeadRequestDto dto) {
        Lead entity = new Lead();
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setClientCategory(dto.getClientCategory());
        entity.setLeadSource(dto.getLeadSource());
        entity.setCreateDeal(dto.getCreateDeal() != null ? dto.getCreateDeal() : false);
        entity.setAutoConvertToClient(dto.getAutoConvertToClient() != null ? dto.getAutoConvertToClient() : false);
        entity.setCompanyName(dto.getCompanyName());
        entity.setOfficialWebsite(dto.getOfficialWebsite());
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setOfficePhone(dto.getOfficePhone());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setPostalCode(dto.getPostalCode());
        entity.setCountry(dto.getCountry());
        entity.setCompanyAddress(dto.getCompanyAddress());
        entity.setStatus(LeadStatus.OPEN);
        return entity;
    }

    public LeadResponseDto toDto(Lead lead) {
        LeadResponseDto dto = new LeadResponseDto();
        dto.setId(lead.getId());
        dto.setName(lead.getName());
        dto.setEmail(lead.getEmail());
        dto.setClientCategory(lead.getClientCategory());
        dto.setLeadSource(lead.getLeadSource());
        dto.setLeadOwner(lead.getLeadOwner());
        dto.setAddedBy(lead.getAddedBy());
        dto.setCreateDeal(lead.getCreateDeal());
        dto.setAutoConvertToClient(lead.getAutoConvertToClient());
        dto.setCompanyName(lead.getCompanyName());
        dto.setOfficialWebsite(lead.getOfficialWebsite());
        dto.setMobileNumber(lead.getMobileNumber());
        dto.setOfficePhone(lead.getOfficePhone());
        dto.setCity(lead.getCity());
        dto.setState(lead.getState());
        dto.setPostalCode(lead.getPostalCode());
        dto.setCountry(lead.getCountry());
        dto.setCompanyAddress(lead.getCompanyAddress());
        dto.setStatus(lead.getStatus());
        dto.setCreatedAt(lead.getCreatedAt());
        dto.setUpdatedAt(lead.getUpdatedAt());
        return dto;
    }
}
