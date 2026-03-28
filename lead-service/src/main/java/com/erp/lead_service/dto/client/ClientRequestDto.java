package com.erp.lead_service.dto.client;

import lombok.Data;

@Data
public class ClientRequestDto {
    private String name;
    private String email;
    private String mobile;
    private String country;
    private String gender;
    private String category;
    private String subCategory;
    private String language;
    private Boolean receiveEmail;
    private String skype;
    private String linkedIn;
    private String twitter;
    private String facebook;
    private String companyName;
    private String website;
    private CompanyDto company;
}