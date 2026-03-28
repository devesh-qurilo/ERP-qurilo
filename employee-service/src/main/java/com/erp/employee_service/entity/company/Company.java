package com.erp.employee_service.entity.company;

import com.erp.employee_service.entity.award.AwardIcon;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String companyName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String contactNo;

    private String website;
    private String address;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_id")
    private AwardIcon logo;

    private Boolean isActive;

    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}