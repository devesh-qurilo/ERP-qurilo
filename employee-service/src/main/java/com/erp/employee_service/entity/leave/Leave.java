package com.erp.employee_service.entity.leave;

import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.FileMeta;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leaves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leave {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_type", nullable = false)
    private DurationType durationType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "single_date")
    private LocalDate singleDate;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeaveStatus status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private Employee approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "leave_documents",
            joinColumns = @JoinColumn(name = "leave_id"),
            inverseJoinColumns = @JoinColumn(name = "file_meta_id")
    )
    private List<FileMeta> documents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    // Helper methods
    public boolean isMultipleDays() {
        return durationType == DurationType.MULTIPLE && startDate != null && endDate != null;
    }

    public boolean isSingleDay() {
        return durationType != DurationType.MULTIPLE && singleDate != null;
    }

    public List<LocalDate> getLeaveDates() {
        List<LocalDate> dates = new ArrayList<>();
        if (isMultipleDays()) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                dates.add(current);
                current = current.plusDays(1);
            }
        } else if (isSingleDay()) {
            dates.add(singleDate);
        }
        return dates;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

