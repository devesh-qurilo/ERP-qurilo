package com.erp.lead_service.mapper;

import com.erp.lead_service.dto.FollowupResponseDto;
import com.erp.lead_service.entity.DealFollowUp;
import org.springframework.stereotype.Component;

@Component
public class FollowupMapper {

    public FollowupResponseDto toDto(DealFollowUp followup) {
        if (followup == null) return null;

        FollowupResponseDto dto = new FollowupResponseDto();
        dto.setId(followup.getId());
        dto.setNextDate(followup.getNextDate());
        dto.setStartTime(followup.getStartTime());
        dto.setRemarks(followup.getRemarks());
        dto.setSendReminder(followup.getSendReminder());
        dto.setReminderScheduled(followup.getReminderScheduled());

        if (followup.getDeal() != null) {
            dto.setDealId(followup.getDeal().getId());
        }

        dto.setRemindBefore(followup.getRemindBefore());
        dto.setRemindUnit(followup.getRemindUnit() != null ? followup.getRemindUnit().name() : null);
        dto.setStatus(followup.getStatus() != null ? followup.getStatus().name() : null);

        return dto;
    }
}
