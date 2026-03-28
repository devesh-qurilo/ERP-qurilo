package com.erp.employee_service.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleRSE(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(java.util.Map.of("error", ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(500).body(java.util.Map.of("error", ex.getMessage()));
    }
}
