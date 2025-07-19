package com.teamEWSN.gitdeun.common.oauth.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.oauth.dto.provider.GitHubResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubApiHelper {

    private final WebClient webClient;

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.github.redirect-uri}")
    private String redirectUri;

    /**
     * 인가 코드로 GitHub Access Token을 요청합니다.
     * @param code GitHub에서 받은 인가 코드
     * @return Access Token 문자열
     */
    public String getAccessToken(String code) {
        String tokenUri = "https://github.com/login/oauth/access_token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);

        Map<String, Object> response = webClient.post()
            .uri(tokenUri)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(formData)
            .retrieve()
            // 단순 map이 아닌 정확한 타입 정보를 런타임에도 잃어버리지 않도록 ParameterizedTypeReference를 사용
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();

        if (response == null || response.get("access_token") == null) {
            log.error("GitHub Access Token 발급 실패: {}", response);
            throw new GlobalException(ErrorCode.OAUTH_PROCESSING_ERROR);
        }

        return (String) response.get("access_token");
    }

    /**
     * Access Token으로 GitHub 사용자 정보를 조회합니다.
     * @param accessToken GitHub Access Token
     * @return GitHub 사용자 정보를 담은 DTO
     */
    public GitHubResponseDto getUserInfo(String accessToken) {
        String userInfoUri = "https://api.github.com/user";

        Map<String, Object> attributes = webClient.get()
            .uri(userInfoUri)
            .header(HttpHeaders.AUTHORIZATION, "token " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block();

        if (attributes == null) {
            throw new GlobalException(ErrorCode.OAUTH_COMMUNICATION_FAILED);
        }

        return new GitHubResponseDto(attributes);
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
