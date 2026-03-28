package com.erp.lead_service.service;

import com.erp.lead_service.dto.FollowupRequestDto;
import com.erp.lead_service.dto.FollowupResponseDto;
import com.erp.lead_service.dto.FollowupUpdateRequestDto;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.DealFollowUp;
import com.erp.lead_service.entity.RemindUnit;
import com.erp.lead_service.entity.FollowupStatus;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.mapper.FollowupMapper;
import com.erp.lead_service.repository.DealFollowUpRepository;
import com.erp.lead_service.repository.DealRepository;
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
public class DealFollowupServiceImpl implements DealFollowupService {

    private final DealRepository dealRepository;
    private final DealFollowUpRepository followupRepository;
    private final ReminderSchedulerService reminderScheduler;
    private final JwtUtil jwtUtil;
    private final FollowupMapper followupMapper;

    @Override
    @Transactional
    public FollowupResponseDto addFollowup(Long dealId, FollowupRequestDto dto, String authHeader) {
        String token = extractToken(authHeader);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can add followups to deals");
        }

        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found"));

        DealFollowUp followup = new DealFollowUp();
        followup.setDeal(deal);
        followup.setNextDate(dto.getNextDate());
        followup.setStartTime(dto.getStartTime());
        followup.setRemarks(dto.getRemarks());
        followup.setSendReminder(dto.getSendReminder() != null ? dto.getSendReminder() : false);

        // set remindBefore & unit if present
        if (dto.getRemindBefore() != null) {
            followup.setRemindBefore(dto.getRemindBefore());
        }

        if (dto.getRemindUnit() != null) {
            try {
                followup.setRemindUnit(RemindUnit.valueOf(dto.getRemindUnit().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("remindUnit must be one of DAYS, HOURS, MINUTES");
            }
        }

        // set default status
        followup.setStatus(FollowupStatus.PENDING);

        DealFollowUp saved = followupRepository.save(followup);

        if (Boolean.TRUE.equals(saved.getSendReminder()) && saved.getRemindBefore() != null && saved.getRemindUnit() != null) {
            reminderScheduler.scheduleFollowupReminder(saved, authHeader);
            saved.setReminderScheduled(true);
            followupRepository.save(saved);
        }

        return followupMapper.toDto(saved);
    }

    @Override
    public List<FollowupResponseDto> listFollowups(Long dealId, String authHeader) {
        String token = extractToken(authHeader);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can view followups");
        }

        List<DealFollowUp> followups = followupRepository.findByDealIdOrderByNextDateAsc(dealId);
        return followups.stream()
                .map(followupMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteFollowup(Long dealId, Long followupId, String authHeader) {
        String token = extractToken(authHeader);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can delete followups");
        }

        DealFollowUp followup = followupRepository.findById(followupId)
                .orElseThrow(() -> new ResourceNotFoundException("Followup not found"));

        if (followup.getDeal() == null || !followup.getDeal().getId().equals(dealId)) {
            throw new ResourceNotFoundException("Followup not found for this deal");
        }

        // Cancel scheduled reminder if any
        try {
            if (Boolean.TRUE.equals(followup.getReminderScheduled())) {
                reminderScheduler.cancelScheduledFollowup(followup);
            }
        } catch (Exception e) {
            log.warn("Failed to cancel scheduled reminder for followup {}: {}", followupId, e.getMessage());
        }

        followupRepository.delete(followup);
    }

    @Transactional
    @Override
    public FollowupResponseDto updateFollowup(Long dealId, Long followupId, FollowupUpdateRequestDto dto, String authHeader) {
        String token = extractToken(authHeader);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can update followups");
        }

        DealFollowUp followup = followupRepository.findById(followupId)
                .orElseThrow(() -> new ResourceNotFoundException("Followup not found"));

        if (followup.getDeal() == null || !followup.getDeal().getId().equals(dealId)) {
            throw new ResourceNotFoundException("Followup not found for this deal");
        }

        if (dto.getNextDate() != null) followup.setNextDate(dto.getNextDate());
        if (dto.getStartTime() != null) followup.setStartTime(dto.getStartTime());
        if (dto.getRemarks() != null) followup.setRemarks(dto.getRemarks());
        if (dto.getSendReminder() != null) followup.setSendReminder(dto.getSendReminder());

        if (dto.getRemindBefore() != null) followup.setRemindBefore(dto.getRemindBefore());
        if (dto.getRemindUnit() != null) {
            try {
                followup.setRemindUnit(RemindUnit.valueOf(dto.getRemindUnit().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("remindUnit must be one of DAYS, HOURS, MINUTES");
            }
        }

        // Update status if provided
        if (dto.getStatus() != null) {
            try {
                followup.setStatus(FollowupStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("status must be one of PENDING, CANCELLED, COMPLETED");
            }
        }

        // Save and (re)schedule reminder appropriately
        DealFollowUp saved = followupRepository.save(followup);

        // If sendReminder true and remindBefore/unit provided -> schedule
        if (Boolean.TRUE.equals(saved.getSendReminder()) && saved.getRemindBefore() != null && saved.getRemindUnit() != null) {
            try {
                reminderScheduler.cancelScheduledFollowup(saved); // cancel existing if any
            } catch (Exception e) {
                log.debug("No existing scheduled job or cancel failed: {}", e.getMessage());
            }
            reminderScheduler.scheduleFollowupReminder(saved, authHeader);
            saved.setReminderScheduled(true);
            followupRepository.save(saved);
        } else {
            // if reminders are turned off, cancel any existing
            if (!Boolean.TRUE.equals(saved.getSendReminder()) && Boolean.TRUE.equals(saved.getReminderScheduled())) {
                try {
                    reminderScheduler.cancelScheduledFollowup(saved);
                } catch (Exception e) {
                    log.warn("Failed to cancel scheduled reminder for followup {}: {}", saved.getId(), e.getMessage());
                }
                saved.setReminderScheduled(false);
                followupRepository.save(saved);
            }
        }

        return followupMapper.toDto(saved);
    }

    @Override
    public FollowupResponseDto getFollowup(Long dealId, Long followupId, String authHeader) {
        String token = extractToken(authHeader);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can view followups");
        }

        DealFollowUp followup = followupRepository.findById(followupId)
                .orElseThrow(() -> new ResourceNotFoundException("Followup not found"));

        if (followup.getDeal() == null || !followup.getDeal().getId().equals(dealId)) {
            throw new ResourceNotFoundException("Followup not found for this deal");
        }

        return followupMapper.toDto(followup);
    }

    private String extractToken(String authHeader) {
        if (authHeader == null) return null;
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }
}
