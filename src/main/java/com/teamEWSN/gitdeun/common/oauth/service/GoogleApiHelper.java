package com.teamEWSN.gitdeun.common.oauth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class GoogleApiHelper {

    private final WebClient webClient;

    public Mono<Void> revokeToken(String accessToken) {
        String revokeUrl = "https://accounts.google.com/o/oauth2/revoke";
        return webClient.post()
            .uri(uriBuilder -> uriBuilder.path(revokeUrl).queryParam("token", accessToken).build())
            .retrieve()
            .bodyToMono(Void.class);
    }
}
