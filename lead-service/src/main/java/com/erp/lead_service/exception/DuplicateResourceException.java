package com.erp.lead_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when trying to create or update a resource
 * that already exists (e.g., duplicate email/mobile).
 */
@ResponseStatus(HttpStatus.CONFLICT) // 409
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
