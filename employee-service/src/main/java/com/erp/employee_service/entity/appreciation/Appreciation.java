package com.erp.employee_service.entity.appreciation;

import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.FileMeta;
import com.erp.employee_service.entity.award.Award;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "appreciations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appreciation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "award_id", nullable = false)
    private Award award;

    @ManyToOne
    @JoinColumn(name = "given_to_employee_id", nullable = false)
    private Employee givenTo;

    @Column(nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @OneToOne
    @JoinColumn(name = "photo_file_id")
    private FileMeta photo;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}