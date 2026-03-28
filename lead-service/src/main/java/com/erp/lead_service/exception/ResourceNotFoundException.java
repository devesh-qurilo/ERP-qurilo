package com.erp.lead_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (Lead, Employee, etc.) is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // 404
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
