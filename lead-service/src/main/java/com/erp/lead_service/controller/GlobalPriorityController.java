package com.erp.lead_service.controller;

import com.erp.lead_service.dto.PriorityDto;
import com.erp.lead_service.service.PriorityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deals/admin/priorities")
@RequiredArgsConstructor
public class GlobalPriorityController {

    private final PriorityService service;

    @GetMapping
    public ResponseEntity<List<PriorityDto>> getAllGlobal(
            @RequestHeader(value = "Authorization") String auth) {
        return ResponseEntity.ok(service.getAllGlobal(auth));
    }

    @PostMapping
    public ResponseEntity<PriorityDto> createGlobal(
            @RequestBody PriorityDto dto,
            @RequestHeader(value = "Authorization") String auth) {
        return ResponseEntity.ok(service.createGlobal(dto, auth));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PriorityDto> updateGlobal(
            @PathVariable Long id,
            @RequestBody PriorityDto dto,
            @RequestHeader(value = "Authorization") String auth) {
        return ResponseEntity.ok(service.updateGlobal(id, dto, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGlobal(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization") String auth) {
        service.deleteGlobal(id, auth);
        return ResponseEntity.noContent().build();
    }
}
