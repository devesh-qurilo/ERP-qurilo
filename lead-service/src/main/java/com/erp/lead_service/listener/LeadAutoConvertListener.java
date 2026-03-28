package com.erp.lead_service.listener;

import com.erp.lead_service.entity.Lead;
import com.erp.lead_service.event.LeadAutoConvertEvent;
import com.erp.lead_service.repository.LeadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeadAutoConvertListener {

    private final LeadRepository leadRepository;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${client-service.url}")
    private String clientServiceBaseUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LeadAutoConvertEvent event) {
        Long leadId = event.getLeadId();
        String authHeader = event.getAuthHeader();
        log.info("LeadAutoConvertListener triggered for leadId={} (auth present={})", leadId, authHeader != null);

        try {
            Lead lead = leadRepository.findById(leadId).orElse(null);
            if (lead == null) {
                log.warn("Lead {} not found — abort conversion", leadId);
                return;
            }
            if (!Boolean.TRUE.equals(lead.getAutoConvertToClient())) {
                log.info("Lead {} autoConvertToClient=false — skipping", leadId);
                return;
            }

            // Build client DTO JSON
            com.erp.lead_service.dto.client.ClientRequestDto clientDto = new com.erp.lead_service.dto.client.ClientRequestDto();
            clientDto.setName(lead.getName());
            clientDto.setEmail(lead.getEmail());
            clientDto.setMobile(lead.getMobileNumber());
            clientDto.setCountry(lead.getCountry());
            clientDto.setCategory(lead.getClientCategory());
            clientDto.setCompanyName(lead.getCompanyName());
            clientDto.setWebsite(lead.getOfficialWebsite());

            String clientJson = objectMapper.writeValueAsString(clientDto);
            log.debug("Client JSON for lead {}: {}", leadId, clientJson);

            // Prepare multipart with content-type application/json for 'client' part
            MultipartBodyBuilder mb = new MultipartBodyBuilder();
            mb.part("client", clientJson)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            WebClient webClient = webClientBuilder.baseUrl(clientServiceBaseUrl).build();

            WebClient.RequestBodySpec request = webClient.post()
                    .uri("/clients")
                    .contentType(MediaType.MULTIPART_FORM_DATA);

            if (authHeader != null && !authHeader.isBlank()) {
                request = request.header("Authorization", authHeader);
            }

            Mono<ClientResponse> respMono = request.body(BodyInserters.fromMultipartData(mb.build()))
                    .exchangeToMono(Mono::just)
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(ex -> {
                        log.error("HTTP call to client-service failed for lead {}: {}", leadId, ex.getMessage());
                        return Mono.empty();
                    });

            ClientResponse resp = respMono.block();
            if (resp == null) {
                log.error("No response received from client-service for lead {}", leadId);
                return;
            }

            int status = resp.statusCode().value();
            String body = resp.bodyToMono(String.class).defaultIfEmpty("").block(Duration.ofSeconds(5));
            log.info("client-service response for lead {} -> status: {}, body: {}", leadId, status, body);

            if (status >= 200 && status < 300) {
                try {
                    lead.setStatus(com.erp.lead_service.entity.LeadStatus.CONVERTED);
                    leadRepository.save(lead);
                    log.info("Lead {} marked CONVERTED after client creation", leadId);
                } catch (Exception e) {
                    log.error("Failed to update lead status after successful client creation for lead {}: {}", leadId, e.getMessage(), e);
                }
            } else {
                log.warn("Client-service returned non-2xx for lead {}: {} — body: {}", leadId, status, body);
            }

        } catch (Exception ex) {
            log.error("Unhandled error in LeadAutoConvertListener for lead {}: {}", leadId, ex.getMessage(), ex);
        }
    }
}
