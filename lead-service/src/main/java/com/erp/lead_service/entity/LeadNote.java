package com.erp.lead_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lead_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @NotBlank
    private String noteTitle;

    @Enumerated(EnumType.STRING)
    private NoteType noteType;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String noteDetails;

    private String createdBy; // employeeId

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
