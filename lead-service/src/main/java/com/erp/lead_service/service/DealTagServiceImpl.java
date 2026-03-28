package com.erp.lead_service.service;

import com.erp.lead_service.dto.TagRequestDto;
import com.erp.lead_service.dto.TagResponseDTO;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.DealTag;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.repository.DealRepository;
import com.erp.lead_service.repository.DealTagRepository;
import com.erp.lead_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DealTagServiceImpl implements DealTagService {

    private final DealTagRepository tagRepo;
    private final DealRepository dealRepo;
    private final JwtUtil jwtUtil;
    public record TagRecord(Long id, String tagName) {}


//    @Override
//    public List<String> getTagsForDeal(Long dealId, String auth) {
//        if (!jwtUtil.isAdmin(auth.substring(7))) {
//            throw new UnauthorizedAccessException("Only admins can add notes to leads");
//        }
//        return tagRepo.findByDealId(dealId).stream().map(DealTag::getTagName).collect(Collectors.toList());
//    }

    // Service method में
    @Override
    public List<TagResponseDTO> getTagsForDeal(Long dealId, String auth) {
        if (!jwtUtil.isAdmin(auth.substring(7))) {
            throw new UnauthorizedAccessException("Only admins can add notes to leads");
        }

        return tagRepo.findByDealId(dealId).stream()
                .map(dealTag -> TagResponseDTO.builder()
                        .id(dealTag.getId())
                        .tagName(dealTag.getTagName())
                        .createdAt(dealTag.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addTag(Long dealId, TagRequestDto dto, String authHeader) {
        if (!jwtUtil.isAdmin(authHeader.substring(7))) {
            throw new UnauthorizedAccessException("Only admins can add notes to leads");
        }
        Deal deal = dealRepo.findById(dealId).orElseThrow(() -> new ResourceNotFoundException("Deal not found"));
        DealTag tag = new DealTag();
        tag.setDeal(deal);
        tag.setTagName(dto.getTagName());
        tagRepo.save(tag);
    }

    @Override
    @Transactional
    public void removeTag(Long dealId, Long tagId, String authHeader) {
        if (!jwtUtil.isAdmin(authHeader.substring(7))) {
            throw new UnauthorizedAccessException("Only admins can add notes to leads");
        }
        DealTag tag = tagRepo.findById(tagId).orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        if (tag.getDeal() == null || !tag.getDeal().getId().equals(dealId)) {
            throw new ResourceNotFoundException("Tag not found for this deal");
        }
        tagRepo.delete(tag);
    }
}
