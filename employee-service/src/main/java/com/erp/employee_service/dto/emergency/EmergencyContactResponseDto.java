package com.erp.employee_service.dto.emergency;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmergencyContactResponseDto {
    private Long id;
    private String name;
    private String email;
    private String mobile;
    private String relationship;
    private String address;

    // Yeh sirf response me rahega, request me nahi
    private String employeeId;
}
