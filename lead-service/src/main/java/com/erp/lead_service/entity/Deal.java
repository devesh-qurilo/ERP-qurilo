package com.erp.lead_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deals")
@Data
public class Deal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Double value;
    private String currency;
    private String dealStage;
    private String pipeline;
    private String dealCategory;

    @ManyToOne
    @JoinColumn(name = "lead_id")
    private Lead lead;

    private LocalDate expectedCloseDate;

    private String dealAgent;

    @ElementCollection
    @CollectionTable(name = "deal_watchers", joinColumns = @JoinColumn(name = "deal_id"))
    @Column(name = "watcher")
    private List<String> dealWatchers = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
