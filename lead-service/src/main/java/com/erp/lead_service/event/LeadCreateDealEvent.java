package com.erp.lead_service.event;

import com.erp.lead_service.dto.deal.DealRequestDto;

public class LeadCreateDealEvent {
    private final Long leadId;
    private final DealRequestDto dealRequest;
    private final String authHeader;

    public LeadCreateDealEvent(Long leadId, DealRequestDto dealRequest, String authHeader) {
        this.leadId = leadId;
        this.dealRequest = dealRequest;
        this.authHeader = authHeader;
    }

    public Long getLeadId() { return leadId; }
    public DealRequestDto getDealRequest() { return dealRequest; }
    public String getAuthHeader() { return authHeader; }
}
