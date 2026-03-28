package com.erp.employee_service.dto.notification;

import lombok.Data;

/**
 * Simple DTO for mark-read request.
 * Using Boolean so getter is getRead() — avoids isRead() / Lombok differences.
 */
@Data
public class MarkReadDto {
    /**
     * true = mark as read, false = mark as unread
     * if null, controller will default to true
     */
    private Boolean read;
}
