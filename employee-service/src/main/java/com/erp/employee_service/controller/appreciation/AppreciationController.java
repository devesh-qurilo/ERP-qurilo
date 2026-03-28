package com.erp.employee_service.controller.appreciation;

import com.erp.employee_service.dto.appreciation.AppreciationRequestDto;
import com.erp.employee_service.dto.appreciation.AppreciationResponseDto;
import com.erp.employee_service.service.appreciation.AppreciationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/employee")
public class AppreciationController {

    private final AppreciationService service;

    // admin-only create (route under /admin for gateway)
    @PostMapping(path = "/appreciations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public AppreciationResponseDto create(
            Authentication auth,
            @ModelAttribute @Valid AppreciationRequestDto dto
    ) {
        String adminEmployeeId = auth.getName();
        return service.create(adminEmployeeId, dto);
    }

    // list all (admin or employee can read; gateway can route /appreciations for general users)
    @GetMapping("/appreciations")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<AppreciationResponseDto> getAll() {
        return service.getAll();
    }

    // get all for a particular employee (self or admin)
    @GetMapping("/appreciations/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<AppreciationResponseDto> getForEmployee(@PathVariable String employeeId, Authentication auth) {
        // if non-admin, ensure they can only access their own records
        if (auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            if (!auth.getName().equals(employeeId)) {
                throw new org.springframework.security.access.AccessDeniedException("Not allowed");
            }
        }
        return service.getForEmployee(employeeId);
    }

    @GetMapping("/appreciations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public AppreciationResponseDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping(path = "/admin/appreciations/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public AppreciationResponseDto update(
            Authentication auth,
            @PathVariable Long id,
            @ModelAttribute @Valid AppreciationRequestDto dto
    ) {
        return service.update(auth.getName(), id, dto);
    }

    @DeleteMapping("/admin/appreciations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Authentication auth, @PathVariable Long id) {
        service.delete(auth.getName(), id);
    }
}