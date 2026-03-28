package com.erp.employee_service.controller.promotion;

import com.erp.employee_service.dto.promotion.PromotionRequestDto;
import com.erp.employee_service.dto.promotion.PromotionResponseDto;
import com.erp.employee_service.dto.promotion.PromotionUpdateDto;
import com.erp.employee_service.service.promotion.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponseDto> createPromotion(
            @PathVariable String employeeId,
            @Valid @RequestBody PromotionRequestDto requestDto) {
        PromotionResponseDto response = promotionService.createPromotion(employeeId, requestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromotionResponseDto>> getAllPromotions() {
        List<PromotionResponseDto> promotions = promotionService.getAllPromotions();
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromotionResponseDto>> getPromotionsByEmployee(@PathVariable String employeeId) {
        List<PromotionResponseDto> promotions = promotionService.getPromotionsByEmployee(employeeId);
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponseDto> getPromotionById(@PathVariable Long id) {
        PromotionResponseDto promotion = promotionService.getPromotionById(id);
        return ResponseEntity.ok(promotion);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponseDto> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionUpdateDto updateDto) {
        PromotionResponseDto response = promotionService.updatePromotion(id, updateDto);
        return ResponseEntity.ok(response);
    }
}