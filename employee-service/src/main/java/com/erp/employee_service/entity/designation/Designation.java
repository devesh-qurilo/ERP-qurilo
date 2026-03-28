package com.erp.employee_service.entity.designation;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "designations")
public class Designation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Column(nullable = false, unique = true)
    private String designationName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_designation_id")
    private Designation parentDesignation;
    @CreationTimestamp
    private LocalDate createDate;
}
