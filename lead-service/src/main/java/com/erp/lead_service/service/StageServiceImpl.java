package com.erp.lead_service.service;

import com.erp.lead_service.dto.stage.StageRequestDto;
import com.erp.lead_service.dto.stage.StageResponseDto;
import com.erp.lead_service.entity.Stage;
import com.erp.lead_service.exception.DuplicateResourceException;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.repository.StageRepository;
import com.erp.lead_service.service.StageService;
import com.erp.lead_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StageServiceImpl implements StageService {

    private final StageRepository stageRepository;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public StageResponseDto createStage(StageRequestDto dto, String authHeader) {
        String token = extractToken(authHeader);
        if (token == null || !jwtUtil.isAdmin(token)) throw new UnauthorizedAccessException("Only admins can create stages");

        if (stageRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Stage already exists: " + dto.getName());
        }

        Stage s = new Stage();
        s.setName(dto.getName().trim());
        Stage saved = stageRepository.save(s);
        return map(saved);
    }

    @Override
    @Transactional
    public StageResponseDto updateStage(Long id, StageRequestDto dto, String authHeader) {
        String token = extractToken(authHeader);
        if (token == null || !jwtUtil.isAdmin(token)) throw new UnauthorizedAccessException("Only admins can update stages");

        Stage s = stageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Stage not found"));
        if (dto.getName() != null && !dto.getName().isBlank()) {
            if (!s.getName().equalsIgnoreCase(dto.getName()) && stageRepository.existsByNameIgnoreCase(dto.getName())) {
                throw new DuplicateResourceException("Stage already exists: " + dto.getName());
            }
            s.setName(dto.getName().trim());
        }
        Stage updated = stageRepository.save(s);
        return map(updated);
    }

    @Override
    @Transactional
    public void deleteStage(Long id, String authHeader) {
        String token = extractToken(authHeader);
        if (token == null || !jwtUtil.isAdmin(token)) throw new UnauthorizedAccessException("Only admins can delete stages");
        if (!stageRepository.existsById(id)) throw new ResourceNotFoundException("Stage not found");
        stageRepository.deleteById(id);
    }

    @Override
    public List<StageResponseDto> listStages() {
        return stageRepository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public StageResponseDto getStage(Long id) {
        Stage s = stageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Stage not found"));
        return map(s);
    }

    private StageResponseDto map(Stage s) {
        StageResponseDto dto = new StageResponseDto();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setUpdatedAt(s.getUpdatedAt());
        return dto;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null) return null;
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }
}
