package com.erp.employee_service.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateEmployeeRequest {
    private String employeeId;
    private String name;

    @Email
    private String email;

    private String password;

    private String profilePictureUrl;

    private String gender;
    private LocalDate birthday;
    private String bloodGroup;
    private LocalDate joiningDate;
    private String language;
    private String country;
    private String mobile;
    private String address;
    private String about;

    private Long departmentId;
    private Long designationId;
    private String reportingToId;

    private String role;
    private Boolean loginAllowed;
    private Boolean receiveEmailNotification;

    private Double hourlyRate;
    private String slackMemberId;
    private List<String> skills;

    private LocalDate probationEndDate;
    private LocalDate noticePeriodStartDate;
    private LocalDate noticePeriodEndDate;

    private String employmentType;
    private String maritalStatus;
    private String businessAddress;
    private String officeShift;
}
