package com.erp.project_service.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shape from client-service (as you specified)
 * Example:
 * {
 *   "clientId": 123,
 *   "name": "ACME Corp",
 *   "profileUrl": "https://..."
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientMetaDto {
    private String clientId;
    private String name;
    private String profilePictureUrl;
}
