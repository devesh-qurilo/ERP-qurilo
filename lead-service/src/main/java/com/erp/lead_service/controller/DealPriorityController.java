package com.erp.lead_service.controller;

import com.erp.lead_service.dto.PriorityAssignDto;
import com.erp.lead_service.dto.PriorityDto;
import com.erp.lead_service.service.PriorityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deals/{dealId}/priority")
@RequiredArgsConstructor
public class DealPriorityController {

    private final PriorityService service;

    @GetMapping
    public ResponseEntity<PriorityDto> getDealPriority(
            @PathVariable Long dealId,
            @RequestHeader(value = "Authorization") String auth) {
        return ResponseEntity.ok(service.getByDeal(dealId, auth));
    }

    @PostMapping("/assign")
    public ResponseEntity<PriorityDto> assignPriority(
            @PathVariable Long dealId,
            @RequestBody PriorityAssignDto dto,
            @RequestHeader(value = "Authorization") String auth) {
        return ResponseEntity.ok(service.assignToDeal(dealId, dto, auth));
    }

    @PutMapping
    public ResponseEntity<PriorityDto> updateDealPriority(
            @PathVariable Long dealId,
            @RequestBody PriorityAssignDto dto,
            @RequestHeader(value = "Authorization") String auth) {
        return ResponseEntity.ok(service.updateDealPriority(dealId, dto, auth));
    }

    @DeleteMapping
    public ResponseEntity<Void> removePriority(
            @PathVariable Long dealId,
            @RequestHeader(value = "Authorization") String auth) {
        service.removeFromDeal(dealId, auth);
        return ResponseEntity.noContent().build();
    }
}