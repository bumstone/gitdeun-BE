package com.teamEWSN.gitdeun.common.oauth.service;

import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.oauth.dto.GitHubEmailDto;
import com.teamEWSN.gitdeun.common.oauth.dto.provider.GitHubResponseDto;
import com.teamEWSN.gitdeun.common.oauth.dto.provider.GoogleResponseDto;
import com.teamEWSN.gitdeun.common.oauth.dto.provider.OAuth2ResponseDto;
import com.teamEWSN.gitdeun.common.oauth.entity.OauthProvider;
import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.common.oauth.repository.SocialConnectionRepository;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.common.oauth.dto.CustomOAuth2User;
import com.teamEWSN.gitdeun.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final GitHubApiHelper gitHubApiHelper;
    private final GoogleApiHelper googleApiHelper;
    private final UserService userService;
    private final SocialTokenRefreshService socialTokenRefreshService;
    private final UserRepository userRepository;
    private final SocialConnectionRepository socialConnectionRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User;
        try {
            oAuth2User = super.loadUser(userRequest);
        } catch (Exception e) {
            // 외부 소셜 플랫폼과의 통신 자체에서 에러가 발생한 경우
            log.error("OAuth2 사용자 정보를 불러오는 데 실패했습니다.", e);
            throw new GlobalException(ErrorCode.OAUTH_COMMUNICATION_FAILED);
        }

        User user = processUser(oAuth2User, userRequest);
        return new CustomOAuth2User(user.getId(), user.getRole());
    }

    // @Transactional
    public User processUser(OAuth2User oAuth2User, OAuth2UserRequest userRequest) {
        OAuth2ResponseDto dto = getOAuth2ResponseDto(oAuth2User, userRequest);
        OauthProvider provider = OauthProvider.valueOf(dto.getProvider().toUpperCase());
        String providerId   = dto.getProviderId();
        String accessToken  = userRequest.getAccessToken().getTokenValue();
        String refreshToken = (String) userRequest.getAdditionalParameters().get("refresh_token");

        // 1) 이미 연결된 소셜 계정이면 토큰 갱신 + 프로필 갱신
        var existingConnOpt = socialConnectionRepository.findByProviderAndProviderId(provider, providerId);
        if (existingConnOpt.isPresent()) {
            var conn = existingConnOpt.get();
            socialTokenRefreshService.refreshSocialToken(conn, accessToken, refreshToken);

            // 프로필 갱신 + (handle 보정 포함) — upsertAndGetId가 책임
            Long userId = userService.upsertAndGetId(
                dto.getEmail(), dto.getName(), dto.getProfileImageUrl(), dto.getNickname()
            );
            return userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));
        }

        // 2) 아직 연결 안된 계정 → upsertAndGetId로 (신규/기존) 저장/보정 먼저
        Long userId = userService.upsertAndGetId(
            dto.getEmail(), dto.getName(), dto.getProfileImageUrl(), dto.getNickname()
        );
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 3) 그 다음 소셜 계정 연결만 수행
        connectSocialAccount(user, provider, providerId, accessToken, refreshToken);
        return user;
    }

    // OAuth2 공급자로부터 받은 사용자 정보를 기반으로 OAuth2ResponseDto를 생성(인스턴스 메서드)
    private OAuth2ResponseDto getOAuth2ResponseDto(OAuth2User oAuth2User, OAuth2UserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        Map<String, Object> attr = new HashMap<>(oAuth2User.getAttributes());

        if (registrationId.equalsIgnoreCase("google")) {
            String accessToken = userRequest.getAccessToken().getTokenValue();

            // 만료 시 토큰 리프레시 후 최신 userinfo 재조회
            if (googleApiHelper.isExpired(accessToken)) {
                String refreshToken = (String) userRequest.getAdditionalParameters().get("refresh_token");
                if (refreshToken != null) {
                    var tokenResp = googleApiHelper.refreshToken(refreshToken);
                    accessToken = tokenResp.accessToken();
                }
            }

            // 항상 최신 userinfo로 덮어쓰기 (이름 변경 즉시 반영)
            Map<String, Object> latestInfo = googleApiHelper.fetchLatestUserInfo(accessToken);
            attr.putAll(latestInfo);

            return new GoogleResponseDto(attr);
        }

        if (registrationId.equalsIgnoreCase("github")) {
            /* ① 기본 프로필에 e-mail 없으면 /user/emails 호출 */
            if (attr.get("email") == null) {
                // accessToken 으로 GitHub 보조 API 호출
                List<GitHubEmailDto> emails =
                    gitHubApiHelper.getPrimaryEmails(userRequest.getAccessToken().getTokenValue());
                attr.put("email",
                    emails.stream().filter(GitHubEmailDto::isPrimary)
                        .findFirst().map(GitHubEmailDto::getEmail).orElse(null));
            }
            return new GitHubResponseDto(attr);
        }
        // 지원하지 않는 소셜 로그인 제공자
        throw new GlobalException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }

    private void connectSocialAccount(User user, OauthProvider provider, String providerId, String accessToken, String refreshToken) {
        socialConnectionRepository.findByProviderAndProviderId(provider, providerId)
            .ifPresent(connection -> {
                // 이 소셜 계정이 이미 다른 유저와 연결되어 있다면 예외 발생
                if (!connection.getUser().getId().equals(user.getId())) {
                    throw new GlobalException(ErrorCode.ACCOUNT_ALREADY_LINKED);
                }
            });

        SocialConnection connection = SocialConnection.builder()
            .user(user)
            .provider(provider)
            .providerId(providerId)
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();

        socialConnectionRepository.save(connection);
    }

}
