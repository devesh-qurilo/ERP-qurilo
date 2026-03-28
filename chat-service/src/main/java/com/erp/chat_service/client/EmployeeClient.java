package com.erp.chat_service.client;

import com.erp.chat_service.dto.EmployeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// use employee.service.url from application.yml
//@FeignClient(name = "employee-service", url = "${employee.service.url}")
@FeignClient(
        name = "employee-service",
        contextId = "employeeDataClient",  // Unique context ID
        path = "/employee"
)
public interface EmployeeClient {

    @GetMapping("/meta/{employeeId}")
    EmployeeDTO getMeta(@PathVariable("employeeId") String employeeId);
    // DTO must match exactly the fields you asked for

    // New search endpoint
    @GetMapping("/meta/search")
    List<EmployeeDTO> searchEmployees(@RequestParam("query") String query);
}
