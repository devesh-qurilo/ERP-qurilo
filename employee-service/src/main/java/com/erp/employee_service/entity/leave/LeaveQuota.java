package com.erp.employee_service.entity.leave;

import com.erp.employee_service.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.time.Year;

@Entity
@Table(name = "leave_quota", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "leaveType", "year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private String leaveType;       // CASUAL, SICK, EARNED etc (uppercase)
    private Integer year = Year.now().getValue(); // Current year

    private Integer totalLeaves;    // yearly assigned
    private Integer monthlyLimit;   // per-month cap (if null or zero -> no monthly cap)

    @Builder.Default
    private Integer totalTaken = 0; // number of days consumed so far this year

    @Builder.Default
    private Integer overUtilized = 0; // if taken beyond totalLeaves

    @Builder.Default
    private Integer remainingLeaves = 0; // totalLeaves - totalTaken

    @PrePersist
    @PreUpdate
    public void calculateRemaining() {
        this.remainingLeaves = Math.max(0, this.totalLeaves - this.totalTaken);
        this.overUtilized = Math.max(0, this.totalTaken - this.totalLeaves);
    }
}