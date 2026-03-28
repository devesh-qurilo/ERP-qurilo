package com.erp.client_service.dto.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Mobile is required")
    @Size(max = 50)
    // optionally change pattern to your mobile format
    private String mobile;

    private String country;
    private String gender;
    private String category;
    private String subCategory;

    @Size(max = 50)
    private String language;

    private Boolean receiveEmail;

    // social links
    private String skype;
    private String linkedIn;
    private String twitter;
    private String facebook;

    private CompanyDto company;
}
