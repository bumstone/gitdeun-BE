package com.teamEWSN.gitdeun.common.oauth.service;

import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.user.dto.provider.GitHubResponseDto;
import com.teamEWSN.gitdeun.user.dto.provider.GoogleResponseDto;
import com.teamEWSN.gitdeun.user.dto.provider.OAuth2ResponseDto;
import com.teamEWSN.gitdeun.common.oauth.entity.OauthProvider;
import com.teamEWSN.gitdeun.user.entity.Role;
import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.common.oauth.repository.SocialConnectionRepository;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.common.oauth.entity.CustomOAuth2User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialConnectionRepository socialConnectionRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User;
        try {
            oAuth2User = super.loadUser(userRequest);
        } catch (Exception e) {
            // 외부 소셜 플랫폼과의 통신 자체에서 에러가 발생한 경우
            log.error("OAuth2 사용자 정보를 불러오는 데 실패했습니다.", e);
            throw new GlobalException(ErrorCode.OAUTH_COMMUNICATION_FAILED);
        }

        User user = processUserInTransaction(oAuth2User, userRequest);
        return new CustomOAuth2User(user.getId(), user.getRole());
    }

    @Transactional
    public User processUserInTransaction(OAuth2User oAuth2User, OAuth2UserRequest userRequest) {
        OAuth2ResponseDto oAuth2ResponseDto = getOAuth2ResponseDto(oAuth2User, userRequest);

        // 이메일 정보가 없는 경우 예외 처리 (GitHub 등)
        if (oAuth2ResponseDto.getEmail() == null) {
            throw new GlobalException(ErrorCode.EMAIL_NOT_PROVIDED);
        }

        OauthProvider provider = OauthProvider.valueOf(oAuth2ResponseDto.getProvider().toUpperCase());
        String providerId = oAuth2ResponseDto.getProviderId();
        String accessToken = userRequest.getAccessToken().getTokenValue();
        String refreshToken = (String) userRequest.getAdditionalParameters().get("refresh_token");

        return socialConnectionRepository.findByProviderAndProviderId(provider, providerId)
            .map(connection -> {
                log.info("기존 소셜 계정 정보를 업데이트합니다: {}", provider);
                connection.updateTokens(accessToken, refreshToken);
                return connection.getUser();
            })
            .orElseGet(() -> {
                // 다른 사용자가 이미 해당 이메일을 사용 중인지 확인
                userRepository.findByEmailAndDeletedAtIsNull(oAuth2ResponseDto.getEmail())
                    .ifPresent(existingUser -> {
                        // 이메일은 같지만, 소셜 연동 정보가 없는 경우 -> 계정 연동
                        log.info("기존 회원 계정에 소셜 계정을 연동합니다: {}", provider);
                        connectSocialAccount(existingUser, provider, providerId, accessToken, refreshToken);
                    });
                // 위에서 연동했거나, 완전 신규 유저인 경우를 처리
                // 다시 이메일로 조회하여 최종 유저를 반환하거나 새로 생성
                return userRepository.findByEmailAndDeletedAtIsNull(oAuth2ResponseDto.getEmail())
                    .orElseGet(() -> {
                        log.info("신규 회원 및 소셜 계정을 생성합니다: {}", provider);
                        return createNewUser(oAuth2ResponseDto, provider, providerId, accessToken, refreshToken);
                    });
            });
    }

    private static OAuth2ResponseDto getOAuth2ResponseDto(OAuth2User oAuth2User, OAuth2UserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2ResponseDto oAuth2ResponseDto;
        if (registrationId.equalsIgnoreCase("google")) {
            oAuth2ResponseDto = new GoogleResponseDto(oAuth2User.getAttributes());
        } else if (registrationId.equalsIgnoreCase("github")) {
            oAuth2ResponseDto = new GitHubResponseDto(oAuth2User.getAttributes());
        } else {
            // 지원하지 않는 소셜 로그인 제공자
            throw new GlobalException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
        return oAuth2ResponseDto;
    }

    private User createNewUser(OAuth2ResponseDto response, OauthProvider provider, String providerId, String accessToken, String refreshToken) {
        User newUser = User.builder()
            .email(response.getEmail())
            .name(response.getName())
            .nickname(response.getName() + "_" + UUID.randomUUID().toString().substring(0, 6))
            .profileImage(response.getProfileImageUrl())
            .role(Role.ROLE_USER)
            .build();

        connectSocialAccount(newUser, provider, providerId, accessToken, refreshToken);
        return userRepository.save(newUser);
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
