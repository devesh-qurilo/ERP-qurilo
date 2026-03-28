package com.erp.employee_service.entity.award;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "awards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Award {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String summary;
    private Boolean isActive;
    private String uploadedByEmployeeId;

    // Use the new AwardIcon entity instead of FileMeta
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "icon_id")
    private AwardIcon icon;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}