package com.teamEWSN.gitdeun.common.oauth.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.oauth.dto.GitHubEmailDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GitHubApiHelper {

    private final WebClient webClient;

    public GitHubApiHelper(@Qualifier("oauthWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String clientSecret;


    /**
     * Access Token으로 사용자의 이메일 목록을 조회합니다.
     * GitHub 기본 사용자 정보에 이메일이 포함되지 않은 경우 사용됩니다.
     * @param accessToken GitHub Access Token
     * @return 이메일 정보 DTO 리스트
     */
    public List<GitHubEmailDto> getPrimaryEmails(String accessToken) {
        String emailsUri = "https://api.github.com/user/emails";

        List<GitHubEmailDto> emails = webClient.get()
            .uri(emailsUri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<GitHubEmailDto>>() {})
            .block();

        if (emails == null || emails.isEmpty()) {
            log.error("GitHub 이메일 정보를 가져올 수 없습니다.");
            throw new GlobalException(ErrorCode.OAUTH_COMMUNICATION_FAILED);
        }
        return emails;
    }


    /**
     * GitHub OAuth 토큰을 해지합니다.
     * @param accessToken 해지할 Access Token
     * @return Mono<Void>
     */
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
