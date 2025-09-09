package com.teamEWSN.gitdeun.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024))  // 10MB 제한
            .build();
    }
}