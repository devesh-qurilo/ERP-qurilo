package com.erp.client_service.dto.client;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDto {
    private String companyName;
    private String website;
    private String officePhone;
    private String taxName;
    private String gstVatNo;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String shippingAddress;
    private String companyLogoUrl; // returned in responses
}
