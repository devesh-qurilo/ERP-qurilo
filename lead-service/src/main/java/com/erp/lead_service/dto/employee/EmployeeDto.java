package com.erp.lead_service.dto.employee;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeDto {
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
    private String departmentId;
    private String departmentName;
    private String designationId;
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