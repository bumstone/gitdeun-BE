package com.teamEWSN.gitdeun.common.oauth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GitHubApiHelper {

    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String clientSecret;

    public Mono<Void> revokeToken(String accessToken) {
        String revokeUrl = "https://api.github.com/applications/" + clientId + "/token";

        String credentials = Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8)
        );

        return webClient.post()
            .uri(revokeUrl)
            .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
            .header(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("access_token", accessToken))
            .retrieve()
            .bodyToMono(Void.class);
    }
}
