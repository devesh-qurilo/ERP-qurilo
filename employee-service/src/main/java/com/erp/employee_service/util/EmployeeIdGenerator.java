package com.erp.employee_service.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

//@Component
//public class EmployeeIdGenerator {
//
//    private final JdbcTemplate jdbc;
//
//    public EmployeeIdGenerator(JdbcTemplate jdbc) {
//        this.jdbc = jdbc;
//    }
//
//    // return numeric nextval
//    public Long nextSeq() {
//        return jdbc.queryForObject("SELECT nextval('employee_seq')", Long.class);
//    }
//
//    // convenience: formatted id
//    public String nextId() {
//        Long seq = nextSeq();
//        return String.format("EMP%03d", seq);
//    }
//}

@Component
public class EmployeeIdGenerator {

    private static final AtomicInteger COUNTER =
            new AtomicInteger(1);

    public String generateTempId() {
        return "TEMP-" + String.format("%04d", COUNTER.getAndIncrement());
    }

    public String generateFinalId() {
        return "EMP-" + UUID.randomUUID().toString()
                .substring(0, 8)
                .toUpperCase();
    }
}

