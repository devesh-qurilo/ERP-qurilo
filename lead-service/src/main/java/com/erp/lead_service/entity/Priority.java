package com.erp.lead_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "priorities")
@Getter
@Setter
public class Priority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status;
    private String color;

    // Global priority ke liye (deal-specific nahi)
    private Boolean isGlobal = true;

    // Deal-specific priority ke liye
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;
}
