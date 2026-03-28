package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_notes", indexes = {
        @Index(name = "idx_tasknote_task", columnList = "task_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskNote extends BaseAuditable {

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_public")
    private boolean isPublic = true;

    // If created by employee and private, see business rule: private visible only to that employee
    @Column(name = "owner_employee_id", length = 50)
    private String ownerEmployeeId;

    private String createdBy;
    @CreationTimestamp
    private LocalDateTime createdDate;

    public boolean getIsPublic() {
       return isPublic;
    }
}
