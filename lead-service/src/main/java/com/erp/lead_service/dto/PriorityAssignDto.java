package com.erp.lead_service.dto;

import lombok.Data;

@Data
public class PriorityAssignDto {
    private Long priorityId;  // Global priority ID jo assign karna hai
    private String status;    // Optional: agar custom status chahiye
    private String color;     // Optional: agar custom color chahiye
}