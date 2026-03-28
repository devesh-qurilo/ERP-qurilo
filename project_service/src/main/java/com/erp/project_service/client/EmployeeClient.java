package com.erp.project_service.client;

import com.erp.project_service.dto.common.EmployeeMetaDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// assumes employee-service registered in Eureka as "employee-service"
@FeignClient(
        name = "employee-service",
        contextId = "employeeDataClient",  // Unique context ID
        path = "/employee"
)
public interface EmployeeClient {

    @GetMapping("/meta/{employeeId}")
    EmployeeMetaDto getMeta(@PathVariable("employeeId") String employeeId);

    // optional: batch meta endpoint if you add to employee-service
    // @PostMapping("/meta/batch") List<EmployeeMetaDto> getBatchMeta(@RequestBody List<String> ids);
}
