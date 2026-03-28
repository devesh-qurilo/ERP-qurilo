package com.erp.client_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "client_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clientId;
    private String filename;
    private String url;
    private String mimeType;
    private Long size;
    private Instant uploadedAt;
    private String uploadedBy;
}
