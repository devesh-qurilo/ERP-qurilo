package com.erp.employee_service.dto.emergency;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmergencyContactDto {
    private String name;

    @Email
    private String email;

    private String mobile;

    private String relationship;

    private String address;
}
