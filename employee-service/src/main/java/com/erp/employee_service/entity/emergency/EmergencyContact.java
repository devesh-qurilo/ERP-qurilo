package com.erp.employee_service.entity.emergency;

import com.erp.employee_service.entity.Employee;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "emergency_contact")
public class EmergencyContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String name;

    @Column(nullable = false)
    @NotBlank
    @Email
    private String email;

    @Column(name = "mobile_number", nullable = false)
    private String mobile;

    @Column(nullable = false)
    private String relationship;

    @Column(nullable = false, length = 1000)
    @Size(min = 1, max = 1000)
    private String address;

    // Relation with Employee
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
}
