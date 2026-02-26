package com.swiftbridge.orchestrator.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Configuration
 * 
 * Configures RestTemplate for making HTTP calls to the Core Converter Service.
 * Implements connection pooling and timeouts for optimal performance.
 * 
 * @author SwiftBridge Team
 * @version 1.0.0
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a RestTemplate bean with custom configuration
     * 
     * @param builder the RestTemplateBuilder
     * @return configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(java.time.Duration.ofSeconds(10))
            .setReadTimeout(java.time.Duration.ofSeconds(30))
            .build();
    }
}
