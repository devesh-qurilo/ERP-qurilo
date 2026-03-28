package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subtasks", indexes = {
        @Index(name = "idx_subtask_task", columnList = "task_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subtask extends BaseAuditable {

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_done")
    private boolean isDone = false;
}
