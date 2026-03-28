package com.erp.lead_service.listener;

import com.erp.lead_service.event.LeadCreateDealEvent;
import com.erp.lead_service.event.LeadAutoConvertEvent;
import com.erp.lead_service.dto.deal.DealRequestDto;
import com.erp.lead_service.dto.deal.DealResponseDto;
import com.erp.lead_service.entity.Lead;
import com.erp.lead_service.repository.LeadRepository;
import com.erp.lead_service.service.DealService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeadCreateDealListener {

    private final DealService dealService;
    private final LeadRepository leadRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LeadCreateDealEvent event) {
        Long leadId = event.getLeadId();
        DealRequestDto dto = event.getDealRequest();
        String authHeader = event.getAuthHeader();

        try {
            dto.setLeadId(leadId);

            DealResponseDto created = dealService.createDeal(dto, authHeader);
            if (created != null) {
                log.info("Auto-created deal {} for lead {}", created.getId(), leadId);

                if ("WIN".equalsIgnoreCase(created.getDealStage())) {
                    Lead lead = leadRepository.findById(leadId).orElse(null);
                    if (lead != null && Boolean.TRUE.equals(lead.getAutoConvertToClient())) {
                        // pass full authHeader so downstream listener can authorize with client-service
                        eventPublisher.publishEvent(new LeadAutoConvertEvent(leadId, authHeader));
                        log.info("Published LeadAutoConvertEvent for lead {} after auto-deal WIN", leadId);
                    }
                }
            }
        } catch (FeignException fe) {
            log.error("Feign error auto-creating deal for lead {}: {}", leadId, safe(fe));
        } catch (Exception ex) {
            log.error("Error auto-creating deal for lead {}: {}", leadId, ex.getMessage(), ex);
        }
    }

    private String safe(FeignException e) {
        try { return e.contentUTF8(); } catch (Exception ex) { return e.getMessage(); }
    }
}
