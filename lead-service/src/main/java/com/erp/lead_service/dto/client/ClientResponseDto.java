package com.erp.lead_service.dto.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientResponseDto {
    private Long id;
    private String clientId;
    private String name;
    private String email;
    private String mobile;
    private String country;
    private String gender;
    private String category;
    private String subCategory;
    private String profilePictureUrl;
    private String language;
    private Boolean receiveEmail;
    private String status;
    private String skype;
    private String linkedIn;
    private String twitter;
    private String facebook;
    private CompanyDto company;
    private String companyLogoUrl;
    private String addedBy;
    private String createdAt;
}