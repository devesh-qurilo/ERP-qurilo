package com.erp.employee_service.controller.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @Email @NotBlank String to,
        @NotBlank String message
) {}
