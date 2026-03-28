package com.erp.employee_service.dto.invite;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CompleteRegistrationRequest {

    private String name;
    private String mobile;
    private String gender;
    private LocalDate birthday;
    private String address;
    private String password;
}

