package com.erp.lead_service.dto.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class CompanyDto {
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
}
