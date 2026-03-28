package com.erp.client_service.dto.client;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSearchDto {
    private String search;
    private String status;
    private Integer page;
    private Integer size;
    private String sort; // e.g., createdAt, name
}
