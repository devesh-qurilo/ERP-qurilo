package com.erp.employee_service.controller.company;

import com.erp.employee_service.dto.company.CompanyRequestDto;
import com.erp.employee_service.dto.company.CompanyResponseDto;
import com.erp.employee_service.service.company.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/employee/company")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyResponseDto createCompany(
            Authentication auth,
            @ModelAttribute @Valid CompanyRequestDto dto
    ) {
        String adminEmployeeId = auth.getName();
        return companyService.createCompany(adminEmployeeId, dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public CompanyResponseDto getCompany() {
        return companyService.getCompany();
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyResponseDto updateCompany(
            Authentication auth,
            @ModelAttribute @Valid CompanyRequestDto dto
    ) {
        String adminEmployeeId = auth.getName();
        return companyService.updateCompany(adminEmployeeId, dto);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCompany(Authentication auth) {
        String adminEmployeeId = auth.getName();
        companyService.deleteCompany(adminEmployeeId);
    }
}