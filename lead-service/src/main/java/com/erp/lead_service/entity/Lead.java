package com.erp.lead_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Data
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String clientCategory;
    private String leadSource;

    private String leadOwner;
    private String addedBy;

    private Boolean createDeal = false;
    private Boolean autoConvertToClient = false;

    // company details
    private String companyName;
    private String officialWebsite;
    private String mobileNumber;
    private String officePhone;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    @Column(columnDefinition = "text")
    private String companyAddress;

    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    private LeadStatus status; // e.g., OPEN, CONVERTED

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
