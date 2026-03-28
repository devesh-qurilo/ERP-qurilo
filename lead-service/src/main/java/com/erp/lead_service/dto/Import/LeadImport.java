package com.erp.lead_service.dto.Import;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeadImport {
    private String name;
    private String email;
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
