package com.erp.employee_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Bean
    public WebClient supabaseWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(supabaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    @Bean
    public WebClient pushWebClient(WebClient.Builder b) {
        return b.build();
    }

    //New
    @Bean
    public WebClient authWebClient(
            WebClient.Builder b,
            @Value("${external.auth.internal-base}") String authBaseUrl
    ) {
        return b.baseUrl(authBaseUrl).build();
    }

}
