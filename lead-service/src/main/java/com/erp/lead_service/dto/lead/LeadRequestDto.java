package com.erp.lead_service.dto.lead;

import lombok.Data;

@Data
public class LeadRequestDto {
    private String name;
    private String email;
    private String clientCategory;
    private String leadSource;

    // employee ids
    private String leadOwner; // EMP-xxx
    private String addedBy;   // EMP-xxx (optional override by admin)

    // checkboxes
    private Boolean createDeal;
    private Boolean autoConvertToClient;

    // deal fields (used only if createDeal = true)
    private DealMiniDto deal;

    // company details
    private String companyName;
    private String officialWebsite;
    private String mobileNumber;
    private String officePhone;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String companyAddress;
}
