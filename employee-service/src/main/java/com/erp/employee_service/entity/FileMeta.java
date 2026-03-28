package com.erp.employee_service.entity;

import com.erp.employee_service.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileMeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bucket;
    private String path;
    private String filename;
    private String mime;
    private Long size;
    private String url;
    private String uploadedBy;

    // Direct relation to Employee (nullable=false for documents tied to employee)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // differentiate types: PROFILE / DOCUMENT / OTHER
    private String entityType;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}

