package com.erp.client_service.dto.Import;

import lombok.Getter;
import lombok.Setter;

/**
 * CSV import DTO — only essential fields (10)
 */
@Getter
@Setter
public class ClientImport {
    private String name;
    private String email;
    private String mobile;
    private String country;
    private String companyName;
    private String website;
    private String officePhone;
    private String city;
    private String state;
    private String postalCode;
    private String gstVatNo; // optional
}
