package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCategory extends BaseAuditable {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    private String createdBy;

    @CreationTimestamp
    private LocalDateTime createdDate;

    public TaskCategory(Object o, String category) {
        super();
    }
}
