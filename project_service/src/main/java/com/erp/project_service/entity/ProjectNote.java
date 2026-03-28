package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_notes", indexes = {
        @Index(name = "idx_projectnote_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectNote extends BaseAuditable {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_public")
    private boolean isPublic = true;

    @Column(name = "owner_employee_id", length = 50)
    private String ownerEmployeeId;

    private String createdBy;
    @CreationTimestamp
    private LocalDateTime createdDate;

    public Boolean getIsPublic() {
        return isPublic;
    }
}
