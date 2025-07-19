package com.teamEWSN.gitdeun.common.oauth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.oauth.entity.OauthProvider;
import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import com.teamEWSN.gitdeun.common.oauth.repository.SocialConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;


// 레포 및 마인드맵 호출 시 소셜로그인 토큰 갱신 호출
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialTokenRefreshService {

    private final SocialConnectionRepository socialConnectionRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 파싱을 위한 ObjectMapper

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;


    // oauth 토큰 갱신
    public void refreshSocialToken(Long userId, OauthProvider provider) {
        SocialConnection connection = socialConnectionRepository.findByUserIdAndProvider(userId, provider)
            .orElseThrow(() -> new GlobalException(ErrorCode.SOCIAL_CONNECTION_NOT_FOUND));

        switch (provider) {
            case GOOGLE -> refreshGoogleToken(connection);
            case GITHUB -> {
                log.warn("GitHub는 토큰 갱신을 지원하지 않습니다. 재인증이 필요합니다.");
                throw new GlobalException(ErrorCode.GITHUB_TOKEN_REFRESH_NOT_SUPPORTED);
            }
        }
    }

    private void refreshGoogleToken(SocialConnection connection) {
        if (connection.getRefreshToken() == null) {
            throw new GlobalException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            // Google Token 갱신 API 호출
            String tokenUrl = "https://oauth2.googleapis.com/token";


            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", googleClientId);
            formData.add("client_secret", googleClientSecret);
            formData.add("refresh_token", connection.getRefreshToken());
            formData.add("grant_type", "refresh_token");


            String response = webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(String.class)
                .block();   // 결과를 동기적으로 기다림

            JsonNode tokenNode = objectMapper.readTree(response);

            String newAccessToken = tokenNode.get("access_token").asText();
            // 구글은 리프레시 토큰을 갱신하면 기존 리프레시 토큰을 다시 주지 않는 경우가 대부분
            String newRefreshToken = tokenNode.has("refresh_token") ?
                tokenNode.get("refresh_token").asText() : connection.getRefreshToken();

            connection.updateTokens(newAccessToken, newRefreshToken);
            socialConnectionRepository.save(connection);

            log.info("Google 토큰 갱신 완료: userId={}", connection.getUser().getId());

        } catch (Exception e) {
            log.error("Google 토큰 갱신 실패: userId={}, error={}",
                connection.getUser().getId(), e.getMessage());
            throw new GlobalException(ErrorCode.SOCIAL_TOKEN_REFRESH_FAILED);
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean isTokenValid(String accessToken, OauthProvider provider) {
        try {
            return switch (provider) {
                case GOOGLE -> validateGoogleToken(accessToken);
                case GITHUB -> validateGitHubToken(accessToken);
            };
        } catch (Exception e) {
            log.error("토큰 유효성 검증 실패: provider={}, error={}", provider, e.getMessage());
            return false;
        }
    }

    private boolean validateGoogleToken(String accessToken) {
        String validateUrl = "https://www.googleapis.com/oauth2/v1/tokeninfo";

        try {
            webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(validateUrl)
                    .queryParam("access_token", accessToken)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateGitHubToken(String accessToken) {
        String validateUrl = "https://api.github.com/user";

        try {
            webClient.get()
                .uri(validateUrl)
                .header(HttpHeaders.AUTHORIZATION, "token " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
