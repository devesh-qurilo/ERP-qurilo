package com.erp.employee_service.entity.award;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "award_icon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AwardIcon {
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

    private LocalDateTime uploadedAt = LocalDateTime.now();

}