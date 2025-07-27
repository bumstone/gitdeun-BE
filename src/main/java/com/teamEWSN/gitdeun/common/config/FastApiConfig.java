package com.teamEWSN.gitdeun.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class FastApiConfig {

    @Value("${fastapi.base-url}")
    private String baseUrl;

    // FastAPI 서버와 통신하기 위한 전용 WebClient Bean
    @Bean("fastApiWebClient")
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
}