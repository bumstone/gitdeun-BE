package com.teamEWSN.gitdeun.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // OAuth 및 기타 외부 API 통신을 위한 범용 WebClient Bean
    @Bean("oauthWebClient")
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
