package com.erp.employee_service.controller.department;

import com.erp.employee_service.dto.department.DepartmentCreateDto;
import com.erp.employee_service.dto.department.DepartmentResponseDto;
import com.erp.employee_service.dto.department.DepartmentUpdateDto;
import com.erp.employee_service.service.department.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/departments")
public class DepartmentController {

    private final DepartmentService svc;

    public DepartmentController(DepartmentService svc) {
        this.svc = svc;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DepartmentResponseDto> create(@Valid @RequestBody DepartmentCreateDto dto) {
        return ResponseEntity.ok(svc.create(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponseDto> update(@PathVariable Long id, @RequestBody DepartmentUpdateDto dto) {
        return ResponseEntity.ok(svc.update(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponseDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(svc.getById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DepartmentResponseDto>> list() {
        return ResponseEntity.ok(svc.getAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        svc.delete(id);
        return ResponseEntity.ok(java.util.Map.of("status","success"));
    }
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status","UP"));
    }
}
