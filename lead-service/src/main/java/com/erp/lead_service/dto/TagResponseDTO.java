// TagResponseDTO.java
package com.erp.lead_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TagResponseDTO {
    private Long id;
    private String tagName;
    private LocalDateTime createdAt;
}