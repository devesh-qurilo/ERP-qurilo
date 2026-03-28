package com.erp.auth_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {

	public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory("../")        // repo root relative to auth-service
                .filename(".env")
                .ignoreIfMissing()       // don't crash if absent
                .load();

        // expose to Spring placeholders like ${JWT_SECRET}
        dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        SpringApplication.run(AuthServiceApplication.class, args);
	}

}
