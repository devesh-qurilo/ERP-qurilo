package com.erp.employee_service.config;

import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.service.leave.LeaveQuotaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeaveQuotaDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LeaveQuotaDataSeeder.class);

    private final EmployeeRepository employeeRepository;
    private final LeaveQuotaService leaveQuotaService;

    @Override
    public void run(String... args) {
        Iterable<Employee> all = employeeRepository.findAll();
        int count = 0;
        for (Employee e : all) {
            leaveQuotaService.assignDefaultsIfMissing(e);
            count++;
        }
        log.info("LeaveQuotaDataSeeder processed {} employees (defaults assigned where missing).", count);
    }
}
