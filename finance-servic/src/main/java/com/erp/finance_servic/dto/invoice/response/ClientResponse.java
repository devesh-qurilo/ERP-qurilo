package com.erp.finance_servic.dto.invoice.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientResponse {
    private String clientId;
    private String name;
    private String profilePictureUrl;
    private String email;
    private String mobile;

    /**
     * These flat fields may or may not be present in the client-service response.
     * We'll keep them and provide fallback getters that read from nested `company` if present.
     */
    private String companyName;
    private String address;
    private String country;

    /**
     * Accept nested company object (if client-service returns company DTO nested).
     * Example shape:
     * "company": { "companyName": "...", "address": "...", "city": "...", ... }
     */
    @JsonProperty("company")
    private CompanyDto company;

    // Fallback getter for companyName: prefer flat field, else nested company.companyName
    public String getCompanyName() {
        if (companyName != null && !companyName.isBlank()) return companyName;
        if (company != null && company.getCompanyName() != null && !company.getCompanyName().isBlank())
            return company.getCompanyName();
        return null;
    }

    // Fallback getter for address: prefer flat field, else nested company.address
    public String getAddress() {
        if (address != null && !address.isBlank()) return address;
        if (company != null && company.getAddress() != null && !company.getAddress().isBlank())
            return company.getAddress();
        return null;
    }

    // Fallback getter for country: prefer flat field, else null (company DTO doesn't include country in your model)
    public String getCountry() {
        if (country != null && !country.isBlank()) return country;
        // if your client-service's company DTO later adds country, this will pick it up
        if (company != null && company.getCountry() != null && !company.getCountry().isBlank())
            return company.getCountry();
        return null;
    }

    /**
     * Small DTO to map nested company payload from client-service.
     * Keep fields minimal — add more as needed.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompanyDto {
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
        private String companyLogoUrl;

        // optional: if your client-service ever returns country inside company, include it
        private String country;
    }
}
