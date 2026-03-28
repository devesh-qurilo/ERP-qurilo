package com.erp.employee_service.controller.designation;

import com.erp.employee_service.dto.designation.DesignationCreateDto;
import com.erp.employee_service.dto.designation.DesignationResponseDto;
import com.erp.employee_service.dto.designation.DesignationUpdateDto;
import com.erp.employee_service.service.designation.DesignationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/designations")
public class DesignationController {

    private final DesignationService svc;

    public DesignationController(DesignationService svc) {
        this.svc = svc;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DesignationResponseDto> create(@Valid @RequestBody DesignationCreateDto dto) {
        return ResponseEntity.ok(svc.create(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DesignationResponseDto> update(@PathVariable Long id, @RequestBody DesignationUpdateDto dto) {
        return ResponseEntity.ok(svc.update(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<DesignationResponseDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(svc.getById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DesignationResponseDto>> list() {
        return ResponseEntity.ok(svc.getAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        svc.delete(id);
        return ResponseEntity.ok(java.util.Map.of("status","success"));
    }
}
