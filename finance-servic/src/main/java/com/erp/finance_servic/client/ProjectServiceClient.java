package com.erp.finance_servic.client;

import com.erp.finance_servic.dto.invoice.response.ProjectResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "project-service", path = "/api/projects")
public interface ProjectServiceClient {

    @GetMapping("/{projectId}")
    ProjectResponse getProjectById(
            @PathVariable String projectId,
            @RequestHeader("X-Internal-Api-Key") String internalApiKey
    );
}
