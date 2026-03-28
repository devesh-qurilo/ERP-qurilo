package com.erp.client_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ClientServiceApplication {

	public static void main(String[] args) {
        // monorepo: service dir se repo root = "../"
        Dotenv dotenv = Dotenv.configure()
                .directory("../")
                .filename(".env")
                .ignoreIfMissing()      // prod me missing ho to crash na ho
                .load();

        // Spring ke ${...} placeholders ko resolve karwane ke liye
        dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));

        SpringApplication.run(ClientServiceApplication.class, args);
	}

}
