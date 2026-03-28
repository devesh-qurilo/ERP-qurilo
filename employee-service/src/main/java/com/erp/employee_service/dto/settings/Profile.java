package com.erp.employee_service.dto.settings;

import lombok.Data;
import java.time.LocalDate;

@Data
public class Profile {
    private String name;
    private String email;
    private String profilePictureUrl;
    private String gender;
    private LocalDate birthday;
    private String bloodGroup;
    private String language;
    private String country;
    private String mobile;
    private String address;
    private String about;
    private String slackMemberId;
    private String maritalStatus;
}
