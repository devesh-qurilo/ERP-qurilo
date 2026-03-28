package com.erp.lead_service.controller;

import com.erp.lead_service.dto.stage.StageRequestDto;
import com.erp.lead_service.dto.stage.StageResponseDto;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.service.StageService;
import com.erp.lead_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;
    private final JwtUtil jwtUtil;

    private String extractToken(String authHeader) {
        if (authHeader == null) return null;
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }

    @PostMapping
    public ResponseEntity<StageResponseDto> createStage(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody StageRequestDto dto) {
        return ResponseEntity.ok(stageService.createStage(dto, auth));
    }

    @GetMapping
    public ResponseEntity<List<StageResponseDto>> listStages(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String token = extractToken(auth);
        if (token == null || !jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can view all stages");
        }
        return ResponseEntity.ok(stageService.listStages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StageResponseDto> getStage(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        String token = extractToken(auth);
        if (token == null || !jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can view a stage");
        }
        return ResponseEntity.ok(stageService.getStage(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StageResponseDto> updateStage(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id,
            @Valid @RequestBody StageRequestDto dto) {
        return ResponseEntity.ok(stageService.updateStage(id, dto, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStage(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable Long id) {
        stageService.deleteStage(id, auth);
        return ResponseEntity.noContent().build();
    }
}
