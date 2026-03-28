package com.erp.client_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "client_notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clientId;
    private String title;

    @Lob
    private String detail;

    @Enumerated(EnumType.STRING)
    private NoteType type;

    private String createdBy;
    private Instant createdAt;
}
