package com.erp.employee_service.event;

import com.erp.employee_service.entity.Employee;
import org.springframework.context.ApplicationEvent;

public class EmployeeCreatedEvent extends ApplicationEvent {
    private final Employee employee;

    public EmployeeCreatedEvent(Object source, Employee employee) {
        super(source);
        this.employee = employee;
    }

    public Employee getEmployee() {
        return employee;
    }
}
