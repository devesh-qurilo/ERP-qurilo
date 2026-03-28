package com.erp.employee_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeId; // EMP001

    private String fileName;
    private String fileUrl;
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
