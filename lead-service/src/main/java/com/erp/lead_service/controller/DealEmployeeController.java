package com.erp.lead_service.controller;

import com.erp.lead_service.dto.EmployeeMetaDto;
import com.erp.lead_service.service.DealEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/deals/{dealId}/employees")
@RequiredArgsConstructor
public class DealEmployeeController {

    private final DealEmployeeService service;

    @GetMapping
    public ResponseEntity<List<EmployeeMetaDto>> list(@PathVariable Long dealId,
                                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(service.listEmployees(dealId, auth));
    }

    @PostMapping
    public ResponseEntity<Void> assign(@PathVariable Long dealId, @RequestBody Map<String, Object> body,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        @SuppressWarnings("unchecked")
        var list = (List<String>) body.get("employeeIds");
        service.assignEmployees(dealId, list, auth);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> remove(@PathVariable Long dealId, @PathVariable String employeeId,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        service.removeEmployee(dealId, employeeId, auth);
        return ResponseEntity.noContent().build();
    }
}
