package com.erp.lead_service.service;

import com.erp.lead_service.dto.PriorityAssignDto;
import com.erp.lead_service.dto.PriorityDto;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.Priority;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.repository.DealRepository;
import com.erp.lead_service.repository.PriorityRepository;
import com.erp.lead_service.service.PriorityService;
import com.erp.lead_service.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class PriorityServiceImpl implements PriorityService {

    private final PriorityRepository repo;
    private final DealRepository dealRepository;
    private final JwtUtil jwtUtil;

    // === GLOBAL PRIORITIES ===

    @Override
    @Transactional
    public PriorityDto createGlobal(PriorityDto dto, String auth) {
        checkAdminAccess(auth);

        Priority p = new Priority();
        p.setStatus(dto.getStatus());
        p.setColor(dto.getColor());
        p.setIsGlobal(true);
        p.setDeal(null);

        Priority saved = repo.save(p);
        return toDto(saved);
    }

    @Override
    @Transactional
    public PriorityDto updateGlobal(Long id, PriorityDto dto, String auth) {
        checkAdminAccess(auth);

        Priority p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found with ID: " + id));

        if (!Boolean.TRUE.equals(p.getIsGlobal())) {
            throw new UnauthorizedAccessException("Cannot update deal-specific priority globally");
        }

        if (dto.getStatus() != null) p.setStatus(dto.getStatus());
        if (dto.getColor() != null) p.setColor(dto.getColor());

        Priority saved = repo.save(p);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteGlobal(Long id, String auth) {
        checkAdminAccess(auth);

        Priority p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found with ID: " + id));

        if (!Boolean.TRUE.equals(p.getIsGlobal())) {
            throw new UnauthorizedAccessException("Cannot delete deal-specific priority globally");
        }

        repo.delete(p);
    }

    @Override
    @Transactional
    public List<PriorityDto> getAllGlobal(String auth) {
        checkAdminAccess(auth);

        List<Priority> globalPriorities = repo.findByIsGlobalTrue();
        return globalPriorities.stream()
                .map(priority -> toDto(priority))  // Lambda use karen
                .collect(Collectors.toList());
    }

    // === DEAL-SPECIFIC PRIORITIES ===

    @Override
    @Transactional
    public PriorityDto assignToDeal(Long dealId, PriorityAssignDto dto, String auth) {
        checkAdminAccess(auth);

        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found with ID: " + dealId));

        // Check if deal already has a priority
        List<Priority> existingPriorities = repo.findByDealId(dealId);
        if (!existingPriorities.isEmpty()) {
            throw new UnauthorizedAccessException("Deal already has a priority assigned");
        }

        Priority priorityToAssign;

        if (dto.getPriorityId() != null) {
            // Use existing global priority
            Priority globalPriority = repo.findById(dto.getPriorityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Priority not found with ID: " + dto.getPriorityId()));

            // Create a copy for the deal
            Priority dealPriority = new Priority();
            dealPriority.setStatus(globalPriority.getStatus());
            dealPriority.setColor(globalPriority.getColor());
            dealPriority.setIsGlobal(false);
            dealPriority.setDeal(deal);

            priorityToAssign = repo.save(dealPriority);
        } else {
            // Create new custom priority
            Priority newPriority = new Priority();
            newPriority.setStatus(dto.getStatus());
            newPriority.setColor(dto.getColor());
            newPriority.setIsGlobal(false);
            newPriority.setDeal(deal);

            priorityToAssign = repo.save(newPriority);
        }

        return toDto(priorityToAssign);
    }

    @Override
    @Transactional
    public PriorityDto updateDealPriority(Long dealId, PriorityAssignDto dto, String auth) {
        checkAdminAccess(auth);

        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found with ID: " + dealId));

        // Get existing priority for this deal
        List<Priority> dealPriorities = repo.findByDealId(dealId);
        if (dealPriorities.isEmpty()) {
            throw new ResourceNotFoundException("No priority found for this deal. Please assign a priority first.");
        }

        Priority existingPriority = dealPriorities.get(0);

        if (dto.getPriorityId() != null) {
            // Update with global priority properties
            Priority globalPriority = repo.findById(dto.getPriorityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Priority not found with ID: " + dto.getPriorityId()));

            // Update existing priority with global priority's properties
            existingPriority.setStatus(globalPriority.getStatus());
            existingPriority.setColor(globalPriority.getColor());

        } else {
            // Update with custom properties from DTO
            if (dto.getStatus() != null) {
                existingPriority.setStatus(dto.getStatus());
            }
            if (dto.getColor() != null) {
                existingPriority.setColor(dto.getColor());
            }
        }

        // Save the updated priority
        Priority updatedPriority = repo.save(existingPriority);
        return toDto(updatedPriority);
    }

    @Override
    @Transactional
    public void removeFromDeal(Long dealId, String auth) {
        checkAdminAccess(auth);

        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found with ID: " + dealId));

        List<Priority> dealPriorities = repo.findByDealId(dealId);
        dealPriorities.forEach(repo::delete);
    }

    @Override
    @Transactional
    public PriorityDto getByDeal(Long dealId, String auth) {
        checkAdminAccess(auth);

        List<Priority> dealPriorities = repo.findByDealId(dealId);
        if (dealPriorities.isEmpty()) {
            throw new ResourceNotFoundException("No priority found for this deal");
        }

        return toDto(dealPriorities.get(0));
    }

    // === EXISTING METHODS (Backward Compatibility) ===

    @Override
    @Transactional
    public List<PriorityDto> listByDeal(Long dealId, String auth) {
        checkAdminAccess(auth);

        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found with ID: " + dealId));

        List<Priority> priorities = repo.findByDealId(dealId);
        return priorities.stream()
                .map(priority -> toDto(priority))  // Lambda use karen
                .collect(Collectors.toList());
    }

    // === HELPER METHODS ===

    private void checkAdminAccess(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new UnauthorizedAccessException("Invalid authorization header");
        }
        String token = auth.substring(7);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can manage priorities");
        }
    }

    private PriorityDto toDto(Priority p) {
        PriorityDto d = new PriorityDto();
        d.setId(p.getId());
        d.setStatus(p.getStatus());
        d.setColor(p.getColor());
        d.setDealId(p.getDeal() != null ? p.getDeal().getId() : null);
        d.setIsGlobal(Boolean.TRUE.equals(p.getIsGlobal()));
        return d;
    }
}