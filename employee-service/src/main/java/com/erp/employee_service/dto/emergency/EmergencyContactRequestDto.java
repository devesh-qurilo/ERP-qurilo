package com.erp.employee_service.dto.emergency;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmergencyContactRequestDto {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String mobile;

    @NotBlank
    private String relationship;

    @NotBlank
    @Size(min = 1, max = 1000)
    private String address;
}
