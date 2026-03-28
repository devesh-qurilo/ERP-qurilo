package com.erp.employee_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EmployeeResponse {

    private String employeeId;
    private String name;
    private String email;
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
    private String departmentName;
    private Long designationId;
    private String designationName;
    private String reportingToId;
    private String reportingToName;
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
    private Boolean active;
    private LocalDateTime createdAt;
}
