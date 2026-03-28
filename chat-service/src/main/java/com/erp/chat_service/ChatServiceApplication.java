package com.erp.chat_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
@EnableAsync
public class ChatServiceApplication {

	public static void main(String[] args) {
        // Look one level up from the module dir to the repo root
        Dotenv dotenv = Dotenv.configure()
                .directory("../")        // repo root relative to chat-service
                .filename(".env")
                .ignoreIfMissing()       // don't crash if absent
                .load();

        // expose to Spring placeholders like ${JWT_SECRET}
        dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        SpringApplication.run(ChatServiceApplication.class, args);
	}

}
