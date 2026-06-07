package com.library.management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// config/AppConfig.java
@Configuration
public class AppConfig {

    @Value("${library.isbn-lookup.timeout-seconds:5}")
    private int timeoutSeconds;

    @Bean
    public RestClient restClient() {
        // Configure underlying HTTP client with timeout
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .build();

        return RestClient.builder()
                .requestFactory(
                        new org.springframework.http.client
                                .JdkClientHttpRequestFactory(httpClient))
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "LibraryManagementSystem/1.0")
                .build();
    }
}
