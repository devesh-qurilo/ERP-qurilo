package com.erp.lead_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user tries to access a resource they are not authorized for.
 */
@ResponseStatus(HttpStatus.FORBIDDEN) // 403
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
