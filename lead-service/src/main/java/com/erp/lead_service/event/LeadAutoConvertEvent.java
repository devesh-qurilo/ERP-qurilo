package com.erp.lead_service.event;

public class LeadAutoConvertEvent {
    private final Long leadId;
    private final String authHeader;

    public LeadAutoConvertEvent(Long leadId, String authHeader) {
        this.leadId = leadId;
        this.authHeader = authHeader;
    }

    public Long getLeadId() { return leadId; }
    public String getAuthHeader() { return authHeader; }
}
