package com.erp.project_service.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shape from employee-service (as you specified)
 * Example:
 * {
 *   "employeeId": "EMP001",
 *   "name": "Sagar",
 *   "profileUrl": "https://...",
 *   "designation": "Developer",
 *   "department": "Engineering"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeMetaDto {
    private String employeeId;
    private String name;
    private String profileUrl;
    private String designation;
    private String department;
}
