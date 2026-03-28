package com.erp.client_service.dto.client;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private String profilePictureUrl;   // uploaded profile picture
    private String language;
    private Boolean receiveEmail;
    private String status;
    private String skype;
    private String linkedIn;
    private String twitter;
    private String facebook;
    private CompanyDto company;
    private String companyLogoUrl;      // convenience top-level company logo
    private String addedBy;
    private String createdAt;          // ISO string
}
