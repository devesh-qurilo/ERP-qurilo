package com.erp.employee_service.dto.company;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CompanyRequestDto {
    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid contact number")
    private String contactNo;

    private String website;
    private String address;
    private MultipartFile logoFile;
}