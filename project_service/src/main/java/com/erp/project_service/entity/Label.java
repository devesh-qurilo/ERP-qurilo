package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "labels", indexes = {
        @Index(name = "idx_label_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label extends BaseAuditable {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "color_code", length = 50)
    private String colorCode;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    private String createdBy;

    @CreationTimestamp
    private LocalDateTime createdDate;

}
