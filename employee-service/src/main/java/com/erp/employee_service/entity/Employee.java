package com.erp.employee_service.entity;

import com.erp.employee_service.entity.department.Department;
import com.erp.employee_service.entity.designation.Designation;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class  Employee {
    @Id
    @Column(name = "employee_id", unique = true, nullable = false)
    private String employeeId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;

//    @Column(nullable = false)
    @JsonIgnore // do not serialize password in responses
    private String password;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

//    @Column(nullable = false)
    private String gender;

//    @Column(nullable = false)
    private LocalDate birthday;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(nullable = false, name = "joining_date")
    private LocalDate joiningDate;

//    @Column(nullable = false)
    private String language;

    private String country;

    @Column(unique = true)
    private String mobile;

//    @Column(nullable = false)
    private String address;


    @Column(columnDefinition = "text")
    private String about;

    // Relationships: Department, Designation, reportingTo (another Employee)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation designation;

    // avoid infinite recursion in JSON serialization
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_to_id", referencedColumnName = "employee_id")
    @JsonIgnoreProperties({"reportingTo", "department", "designation"})
    private Employee reportingTo;

    // role string like ROLE_EMPLOYEE or ROLE_ADMIN
    @Column(name = "role")
    private String role = "ROLE_EMPLOYEE";

    @Column(name = "login_allowed")
    @ColumnDefault("true")
    private Boolean loginAllowed = true;

    @Column(name = "receive_email_notification")
    @ColumnDefault("true")
    private Boolean receiveEmailNotification = true;

    @Column(name = "hourly_rate")
    private Double hourlyRate;

    @Column(name = "slack_member_id")
    private String slackMemberId;

    @ElementCollection
    @CollectionTable(name = "employee_skills", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    @Column(name = "notice_period_start_date")
    private LocalDate noticePeriodStartDate;

    @Column(name = "notice_period_end_date")
    private LocalDate noticePeriodEndDate;

    @Column(name = "employment_type")
    private String employmentType; // Full Time, Part Time, Contract

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "business_address")
    private String businessAddress;

    @Column(name = "office_shift")
    private String officeShift;

//    @Column(nullable = false)
    @ColumnDefault("true")
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // convenience helper: mask sensitive info when needed (not persisted)
    @Transient
    public boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(this.role);
    }
}
