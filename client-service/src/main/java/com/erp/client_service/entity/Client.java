package com.erp.client_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "clients", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email"}),
        @UniqueConstraint(columnNames = {"mobile"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", unique = true, nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String mobile;

    private String country;
    private String gender;
    private String category;
    private String subCategory;
    private String profilePictureUrl;
    private String language;
    private Boolean receiveEmail = true;
    private String status;

    // Social links
    private String skype;
    private String linkedIn;
    private String twitter;
    private String facebook;

    // Company logo at client level
    private String companyLogoUrl;

    private String addedBy;
    private Instant createdAt;
    private Instant updatedAt;

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Company company;
}
