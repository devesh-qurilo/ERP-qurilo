// src/main/java/com/erp/project_service/entity/ProjectUserState.java
package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "project_user_state",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id","user_id"}),
        indexes = {
                @Index(name="ix_pus_user_pinned", columnList="user_id,pinned_at"),
                @Index(name="ix_pus_user_archived", columnList="user_id,archived_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectUserState {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="project_id", nullable=false)
    private Long projectId;

    @Column(name="user_id", nullable=false, length=50)
    private String userId;

    @Column(name="pinned_at")
    private Instant pinnedAt;     // null = not pinned

    @Column(name="archived_at")
    private Instant archivedAt;   // null = not archived

    @Transient
    public boolean isPinned() { return pinnedAt != null; }

    @Transient
    public boolean isArchived() { return archivedAt != null; }
}
