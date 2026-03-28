package com.erp.finance_servic.service.external;

import com.erp.finance_servic.client.ClientServiceClient;
import com.erp.finance_servic.client.ProjectServiceClient;
import com.erp.finance_servic.dto.invoice.response.ClientResponse;
import com.erp.finance_servic.dto.invoice.response.ProjectResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalServiceClient {

    private final ProjectServiceClient projectServiceClient;
    private final ClientServiceClient clientServiceClient;

    @Value("${internal.api.key}")
    private String internalApiKey;

//    public ProjectResponse getProjectById(String projectId) {
//        try {
//            return projectServiceClient.getProjectById(projectId, internalApiKey);
//        } catch (Exception e) {
//            log.error("Failed to fetch project by id {} : {}", projectId, e.getMessage(), e);
//            throw new RuntimeException("Failed to fetch project details: " + e.getMessage(), e);
//        }
//    }

    public ProjectResponse getProjectById(String projectId) {
        try {
            return projectServiceClient.getProjectById(projectId, internalApiKey);
        } catch (FeignException fe) {
            int status = fe.status();
            String body = "";
            try { body = fe.contentUTF8(); } catch (Exception ignored) {}
            log.warn("Feign error fetching project {} -> status: {}, body: {}", projectId, status, body);

            // treat 404 as not found
            if (status == 404) {
                return null;
            }

            // some services return 500 with message "Project not found" — handle that too
            if (status >= 500 && body != null && body.toLowerCase().contains("project not found")) {
                return null;
            }

            // all other feign errors: rethrow as runtime to surface properly
            throw new RuntimeException("Failed to fetch project details: " + fe.getMessage(), fe);
        } catch (Exception e) {
            log.error("Failed to fetch project by id {} : {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch project details: " + e.getMessage(), e);
        }
    }

    /**
     * Get client meta by clientId.
     * 1) Try internal endpoint /internal/client/{clientId}
     * 2) If partial result -> fallback to searchClients(search=clientId)
     *    - Prefer exact clientId match from search results
     *    - Else prefer first non-partial (rich) entry
     *    - Else return first search result
     */
    public ClientResponse getClientById(String clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("clientId must not be null");
        }

        try {
            // Primary call: internal endpoint (authoritative)
            ClientResponse client = clientServiceClient.getClientByIdInternal(clientId, internalApiKey);
            log.debug("Primary client fetch for id {} -> partial? {} -> {}", clientId, isPartial(client), client);

            // if primary result is complete enough, return it
            if (!isPartial(client)) {
                return client;
            }

            // fallback: search
            log.warn("Client {} returned partial data, invoking search fallback", clientId);
            List<ClientResponse> list = clientServiceClient.searchClients(clientId, null, internalApiKey);

            if (list == null || list.isEmpty()) {
                log.warn("Search fallback returned empty for '{}', returning primary (partial) result", clientId);
                return client;
            }

            // 1) prefer exact clientId match (case-insensitive)
            for (ClientResponse c : list) {
                if (c == null) continue;
                if (c.getClientId() != null && c.getClientId().equalsIgnoreCase(clientId)) {
                    log.debug("Search fallback: found exact clientId match for {} -> {}", clientId, c);
                    return c;
                }
            }

            // 2) prefer first 'rich' (non-partial) entry
            for (ClientResponse c : list) {
                if (c == null) continue;
                if (!isPartial(c)) {
                    log.debug("Search fallback: found non-partial result for {} -> {}", clientId, c);
                    return c;
                }
            }

            // 3) last resort: return first element
            log.debug("Search fallback: no exact/rich match; returning first search result for {} -> {}", clientId, list.get(0));
            return list.get(0);

        } catch (Exception e) {
            log.error("Failed to fetch client details for id {}: {}", clientId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch client details: " + e.getMessage(), e);
        }
    }

    /**
     * Consider a client 'partial' if core contact/company fields are missing.
     */
    private boolean isPartial(ClientResponse client) {
        if (client == null) return true;
        boolean noEmail = (client.getEmail() == null || client.getEmail().isBlank());
        boolean noCompany = (client.getCompanyName() == null || client.getCompanyName().isBlank());
        boolean noMobile = (client.getMobile() == null || client.getMobile().isBlank());
        return noEmail && noCompany && noMobile;
    }
}
