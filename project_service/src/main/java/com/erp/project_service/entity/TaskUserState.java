package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "task_user_state",
        uniqueConstraints = @UniqueConstraint(columnNames = {"task_id","user_id"}),
        indexes = {
                @Index(name="ix_tus_user_pinned", columnList="user_id,pinned_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskUserState {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="task_id", nullable=false)
    private Long taskId;

    @Column(name="user_id", nullable=false, length=50)
    private String userId;

    @Column(name="pinned_at")
    private Instant pinnedAt; // null = not pinned

    @Transient
    public boolean isPinned() { return pinnedAt != null; }
}
