package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "project_activity", indexes = {
        @Index(name = "idx_activity_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "actor_employee_id", length = 50)
    private String actorEmployeeId;

    @Column(name = "action", length = 200)
    private String action;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON or plain text for extra details

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
