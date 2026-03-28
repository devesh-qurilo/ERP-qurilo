package com.erp.finance_servic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Bean
    @Qualifier("supabaseWebClient")
    public WebClient supabaseWebClient() {
        // normalize base url without trailing slash
        String base = supabaseUrl != null && supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        HttpClient httpClient = HttpClient.create().compress(true);
        return WebClient.builder()
                .baseUrl(base)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
