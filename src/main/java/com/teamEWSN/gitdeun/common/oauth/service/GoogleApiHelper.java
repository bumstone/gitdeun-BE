package com.teamEWSN.gitdeun.common.oauth.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.oauth.dto.GoogleTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;


@Slf4j
@Component
public class GoogleApiHelper {

    private final WebClient webClient;

    public GoogleApiHelper(@Qualifier("oauthWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;


    /**
     * 토큰이 만료되었는지 확인
     * @param accessToken 확인할 액세스 토큰
     * @return 만료 여부 (true: 만료됨, false: 유효함)
     */
    protected boolean isExpired(String accessToken) {
        // String validateUrl = "https://www.googleapis.com/oauth2/v1/tokeninfo";
        try {
            // 토큰 정보 요청
            webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("www.googleapis.com")
                    .path("/oauth2/v1/tokeninfo")
                    .queryParam("access_token", accessToken)
                    .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

            // 응답이 있으면 토큰이 유효함
            return false; // 만료되지 않음
        } catch (WebClientResponseException e) {
            // 401, 400 등의 에러는 토큰이 만료되었거나 유효하지 않음
            log.debug("토큰 검증 오류: {}", e.getMessage());
            return true; // 만료됨
        } catch (Exception e) {
            // 기타 예외
            log.error("토큰 검증 중 예상치 못한 오류: {}", e.getMessage());
            return true; // 안전하게 만료로 취급
        }
    }

    /**
     * 리프레시 토큰으로 새 액세스 토큰 요청
     * @param refreshToken 리프레시 토큰
     * @return 새로운 토큰 응답 객체
     */
    protected GoogleTokenResponse refreshToken(String refreshToken) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        // 요청 바디 구성
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", googleClientId);
        formData.add("client_secret", googleClientSecret);
        formData.add("refresh_token", refreshToken);
        formData.add("grant_type", "refresh_token");

        try {
            // 토큰 갱신 요청
            return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class)
                .block();
        } catch (Exception e) {
            log.error("Google 토큰 갱신 실패: {}", e.getMessage());
            throw new GlobalException(ErrorCode.SOCIAL_TOKEN_REFRESH_FAILED);
        }
    }

    /**
     * access token 으로 최신 프로필(userinfo) 조회
     * (name, picture, email 등)
     */
    public Map<String, Object> fetchLatestUserInfo(String accessToken) {
        try {
            return webClient.get()
                .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        } catch (WebClientResponseException e) {
            log.error("Google userinfo 조회 실패: {}", e.getResponseBodyAsString());
            throw new GlobalException(ErrorCode.OAUTH_COMMUNICATION_FAILED);
        } catch (Exception e) {
            log.error("Google userinfo 조회 중 오류: {}", e.getMessage());
            throw new GlobalException(ErrorCode.OAUTH_COMMUNICATION_FAILED);
        }
    }

    public Mono<Void> revokeToken(String accessToken) {
        String revokeUrl = "https://accounts.google.com/o/oauth2/revoke";
        return webClient.post()
            .uri(uriBuilder -> uriBuilder.path(revokeUrl).queryParam("token", accessToken).build())
            .retrieve()
            .bodyToMono(Void.class);
    }
}
