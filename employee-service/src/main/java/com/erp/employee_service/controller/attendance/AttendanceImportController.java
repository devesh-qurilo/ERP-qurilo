package com.erp.employee_service.controller.attendance;

import com.erp.employee_service.dto.imports.ImportResult;
import com.erp.employee_service.service.imports.AttendanceCsvImportService;
import com.erp.employee_service.util.JwtUtil;
import com.erp.employee_service.util.SecurityUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/employee/attendance/import")
@RequiredArgsConstructor
public class AttendanceImportController {

    private final AttendanceCsvImportService attendanceCsvImportService;
    private final JwtUtil jwtUtil;
    private final SecurityUtils securityUtils;

    /**
     * ✅ ADMIN-only endpoint to import attendance via CSV.
     * Accepts only a file (multipart/form-data).
     * - Authorization: Bearer <JWT>
     * - File field name: "file"
     * - markedBy extracted automatically from JWT (subject claim)
     * - overwrite = false (default)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ImportResult>> importAttendanceCsv(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        String markedBy = null;

        try {
            // Extract employeeId from JWT token
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Claims claims = jwtUtil.validateToken(token);
                markedBy = claims.getSubject(); // JWT subject (usually employeeId)
            }
        } catch (Exception e) {
            // fallback to SecurityContext (in case of custom security)
            markedBy = securityUtils.getCurrentEmployeeId();
        }

        // Default overwrite = false
        List<ImportResult> results = attendanceCsvImportService.importAttendanceFromCsv(file, markedBy, false);

        // Check if there’s any file-level error
        boolean hasFileError = results.stream()
                .anyMatch(r -> r.getRowNumber() == 0 && "ERROR".equalsIgnoreCase(r.getStatus()));

        if (hasFileError) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(results);
        }
        return ResponseEntity.ok(results);
    }
}
