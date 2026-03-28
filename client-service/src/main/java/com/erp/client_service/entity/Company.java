package com.erp.client_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "client_company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "client_id")
    private Client client;

    private String companyName;
    private String website;
    private String officePhone;
    private String taxName;
    private String gstVatNo;

    @Column(columnDefinition = "text")
    private String address;

    private String city;
    private String state;
    private String postalCode;

    @Column(columnDefinition = "text")
    private String shippingAddress;

    private String companyLogoUrl;
}
