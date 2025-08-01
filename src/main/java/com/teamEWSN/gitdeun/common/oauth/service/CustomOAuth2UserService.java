package com.teamEWSN.gitdeun.common.oauth.service;

import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.oauth.dto.GitHubEmailDto;
import com.teamEWSN.gitdeun.common.oauth.dto.provider.GitHubResponseDto;
import com.teamEWSN.gitdeun.common.oauth.dto.provider.GoogleResponseDto;
import com.teamEWSN.gitdeun.common.oauth.dto.provider.OAuth2ResponseDto;
import com.teamEWSN.gitdeun.common.oauth.entity.OauthProvider;
import com.teamEWSN.gitdeun.user.entity.Role;
import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.common.oauth.repository.SocialConnectionRepository;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.common.oauth.dto.CustomOAuth2User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final GitHubApiHelper gitHubApiHelper;
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

        /* ② 이미 연결된 계정 → 토큰 갱신 로직 추상화  */
        return socialConnectionRepository.findByProviderAndProviderId(provider, providerId)
            .map(conn -> {
                // provider 별 refresh 정책
                socialTokenRefreshService.refreshSocialToken(conn, accessToken, refreshToken);
                return conn.getUser();
            })
            .orElseGet(() -> createOrConnect(dto, provider, providerId, accessToken, refreshToken));
    }

    // OAuth2 공급자로부터 받은 사용자 정보를 기반으로 OAuth2ResponseDto를 생성(인스턴스 메서드)
    private OAuth2ResponseDto getOAuth2ResponseDto(OAuth2User oAuth2User, OAuth2UserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attr = oAuth2User.getAttributes();

        if (registrationId.equalsIgnoreCase("google")) {
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
        } else {
            // 지원하지 않는 소셜 로그인 제공자
            throw new GlobalException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }

    /**
     * 사용자가 존재하면 계정을 연결하고, 존재하지 않으면 새로 생성합니다.
     */
    private User createOrConnect(OAuth2ResponseDto response, OauthProvider provider, String providerId, String accessToken, String refreshToken) {
        // 이메일로 기존 사용자를 찾습니다.
        return userRepository.findByEmailAndDeletedAtIsNull(response.getEmail())
            .map(user -> {
                // 사용자가 존재하면, 새 소셜 계정을 연결
                connectSocialAccount(user, provider, providerId, accessToken, refreshToken);
                return user;
            })
            .orElseGet(() -> {
                // 사용자가 존재하지 않으면, 새 사용자를 생성
                return createNewUser(response, provider, providerId, accessToken, refreshToken);
            });
    }

    private User createNewUser(OAuth2ResponseDto response, OauthProvider provider, String providerId, String accessToken, String refreshToken) {
        // provider별 다른 Nickname 처리 로직
        String nickname = response.getNickname();
        if (provider == OauthProvider.GOOGLE) {
            nickname = nickname + "_" + UUID.randomUUID().toString().substring(0, 6);
        }

        User newUser = User.builder()
            .email(response.getEmail())
            .name(response.getName())   // GitHub의 경우 full name, Google의 경우 name
            .nickname(nickname)
            .profileImage(response.getProfileImageUrl())
            .role(Role.USER)
            .build();
        User savedUser = userRepository.save(newUser);

        connectSocialAccount(savedUser, provider, providerId, accessToken, refreshToken);
        return savedUser;
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
