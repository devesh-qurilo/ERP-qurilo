package com.erp.lead_service.service;

import com.erp.lead_service.client.EmployeeServiceClient;
import com.erp.lead_service.dto.CommentRequestDto;
import com.erp.lead_service.dto.CommentResponseDto;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.DealComment;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.mapper.DealMapper;
import com.erp.lead_service.repository.DealCommentRepository;
import com.erp.lead_service.repository.DealRepository;
import com.erp.lead_service.service.DealCommentService;
import com.erp.lead_service.util.JwtUtil;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DealCommentServiceImpl implements DealCommentService {

    private final DealRepository dealRepo;
    private final DealCommentRepository commentRepo;
    private final JwtUtil jwtUtil;
    private final EmployeeServiceClient employeeClient;
    private final DealMapper dealMapper;

    @Override
    @Transactional
    public CommentResponseDto addComment(Long dealId, CommentRequestDto dto, String authHeader) {
        if (!jwtUtil.isAdmin(authHeader.substring(7))) {
            throw new UnauthorizedAccessException("Only admins can access deal comments");
        }
        Deal deal = dealRepo.findById(dealId).orElseThrow(() -> new ResourceNotFoundException("Deal not found"));
        String token = extractToken(authHeader);
        String currentEmployeeId = jwtUtil.extractSubject(token);

        // access: agent/watcher/assigned or admin allowed - use simple check
        // For simplicity: allow comment if admin or whitelisted - production: check properly
        // We'll let service throw if not permitted
        DealComment comment = new DealComment();
        comment.setDeal(deal);
        comment.setCommentText(dto.getCommentText());
        comment.setEmployeeId(currentEmployeeId);
        DealComment saved = commentRepo.save(comment);

        return toDto(saved);
    }

    @Override
    public List<CommentResponseDto> getComments(Long dealId, String authHeader) {
        if (!jwtUtil.isAdmin(authHeader.substring(7))) {
            throw new UnauthorizedAccessException("Only admins can access deal comments");
        }
        return commentRepo.findByDealIdOrderByCreatedAtDesc(dealId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(Long dealId, Long commentId, CommentRequestDto dto, String authHeader) {
        if (!jwtUtil.isAdmin(authHeader.substring(7))) {
            throw new UnauthorizedAccessException("Only admins can access deal comments");
        }
        DealComment comment = commentRepo.findById(commentId).orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (comment.getDeal() == null || !comment.getDeal().getId().equals(dealId)) {
            throw new ResourceNotFoundException("Comment not found on this deal");
        }
        String token = extractToken(authHeader);
        String currentEmployeeId = jwtUtil.extractSubject(token);
        boolean isAdmin = jwtUtil.isAdmin(token);

        if (!isAdmin && !currentEmployeeId.equals(comment.getEmployeeId())) {
            throw new UnauthorizedAccessException("Only admin or comment owner can update comment");
        }

        comment.setCommentText(dto.getCommentText());
        DealComment updated = commentRepo.save(comment);
        return toDto(updated);
    }

    @Override
    @Transactional
    public void deleteComment(Long dealId, Long commentId, String authHeader) {
        if (!jwtUtil.isAdmin(authHeader.substring(7))) {
            throw new UnauthorizedAccessException("Only admins can access deal comments");
        }
        DealComment comment = commentRepo.findById(commentId).orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (comment.getDeal() == null || !comment.getDeal().getId().equals(dealId)) {
            throw new ResourceNotFoundException("Comment not found on this deal");
        }
        String token = extractToken(authHeader);
        String currentEmployeeId = jwtUtil.extractSubject(token);
        boolean isAdmin = jwtUtil.isAdmin(token);

        if (!isAdmin && !currentEmployeeId.equals(comment.getEmployeeId())) {
            throw new UnauthorizedAccessException("Only admin or comment owner can delete comment");
        }

        commentRepo.delete(comment);
    }

    private CommentResponseDto toDto(DealComment c) {
        CommentResponseDto dto = new CommentResponseDto();
        dto.setId(c.getId());
        dto.setEmployeeId(c.getEmployeeId());
        dto.setCommentText(c.getCommentText());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null) return null;
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }
}
