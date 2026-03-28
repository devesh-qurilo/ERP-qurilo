package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "created_by", length = 50, nullable = false)
    protected String createdBy; // employeeId

    @Column(name = "created_at", nullable = false, updatable = false)
    protected Instant createdAt;

    @Column(name = "updated_by", length = 50)
    protected String updatedBy;

    @Column(name = "updated_at")
    protected Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
