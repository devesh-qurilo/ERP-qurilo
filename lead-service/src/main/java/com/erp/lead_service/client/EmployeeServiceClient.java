package com.erp.lead_service.client;

import com.erp.lead_service.dto.EmployeeMetaDto;
import com.erp.lead_service.dto.employee.EmployeeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "employee-service", url = "${employee-service.url}")
public interface EmployeeServiceClient {

    @GetMapping("/employee/{employeeId}")
    EmployeeDto getEmployeeById(@PathVariable("employeeId") String employeeId,
                                @RequestHeader("Authorization") String authorization);


    @GetMapping("/employee/me")
    EmployeeDto getCurrentEmployee(@RequestHeader("Authorization") String authorization);

    @GetMapping("/employee")
    List<EmployeeDto> getAllEmployees(@RequestHeader("Authorization") String authorization,
                                      @RequestParam(value = "page", required = false) Integer page,
                                      @RequestParam(value = "size", required = false) Integer size);

    // light-weight existence endpoint (should be available in employee-service)
    @GetMapping("/employee/exists/{employeeId}")
    Boolean checkEmployeeExists(@PathVariable("employeeId") String employeeId,
                                @RequestHeader(value = "Authorization", required = false) String authorization);

    // NEW: lightweight meta endpoint (no sensitive data). LeadServiceImpl fallback इसी को call करेगा.
    @GetMapping("/employee/meta/{employeeId}")
    EmployeeMetaDto getMeta(@PathVariable("employeeId") String employeeId);
}
