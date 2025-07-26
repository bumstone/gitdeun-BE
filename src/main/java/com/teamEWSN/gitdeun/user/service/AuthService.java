package com.teamEWSN.gitdeun.user.service;


import com.teamEWSN.gitdeun.common.jwt.RefreshToken;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.cookie.CookieUtil;
import com.teamEWSN.gitdeun.common.jwt.*;
import com.teamEWSN.gitdeun.common.oauth.entity.OauthProvider;
import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import com.teamEWSN.gitdeun.common.oauth.repository.SocialConnectionRepository;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final BlacklistService blacklistService;
    private final UserService userService;
    private final CookieUtil cookieUtil;
    private final UserRepository userRepository;
    private final SocialConnectionRepository socialConnectionRepository;

    @Value("${jwt.refresh-expired}")
    private Long refreshTokenExpired;

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String githubClientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String githubClientSecret;

    @Value("${spring.security.oauth2.client.registration.github.redirect-uri}")
    private String githubRedirectUri;

    // 로그 아웃
    @Transactional
    public void logout(String accessToken, String refreshToken, HttpServletResponse response) {
        blacklistService.addToBlacklist(accessToken);
        refreshTokenService.deleteRefreshToken(refreshToken);
        cookieUtil.deleteCookie(response, "refreshToken");
    }

    // 토큰 재발급
    @Transactional
    public JwtToken refreshTokens(String refreshToken) {
        // Redis에서 Refresh Token 조회
        RefreshToken tokenDetails = refreshTokenService.getRefreshToken(refreshToken)
            .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 토큰에서 email 정보 추출
        String email = tokenDetails.getEmail();

        // email로 사용자 정보 조회
        User user = userService.findUserByEmail(email);
        Authentication authentication = createAuthentication(user);

        // 기존 리프레시 토큰은 DB에서 제거 (순환)
        refreshTokenService.deleteRefreshToken(refreshToken);

        // 새로운 Access/Refresh 토큰 생성
        return jwtTokenProvider.generateToken(authentication);
    }

    // User 객체를 사용해 authentication 생성
    private Authentication createAuthentication(User user) {
        CustomUserDetails customUserDetails = new CustomUserDetails(
            user.getId(), user.getEmail(), user.getNickname(), user.getProfileImage(), user.getRole(), user.getName());

        return new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
    }


    // 구글 -> Github 계정 연동
    public void connectGithubAccount(OAuth2User githubUser, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        String providerId = Objects.requireNonNull(githubUser.getAttribute("id")).toString();
        String accessToken = githubUser.getAttribute("access_token");
        String refreshToken = githubUser.getAttribute("refresh_token");

        // 이미 다른 계정에 연동되어 있는지, 동일 사용자에 의해 이미 연동되는지 확인
        socialConnectionRepository.findByProviderAndProviderId(OauthProvider.GITHUB, providerId)
            .ifPresent(connection -> {
                if (!connection.getUser().getId().equals(userId)) {
                    throw new GlobalException(ErrorCode.ACCOUNT_ALREADY_LINKED);
                } else {
                    log.info("이미 현재 사용자와 연동된 GitHub 계정입니다: {}", providerId);
                    return;
                }
            });


        // 신규 소셜 연동 정보 생성 및 저장
        SocialConnection connection = SocialConnection.builder()
            .user(user)
            .provider(OauthProvider.GITHUB)
            .providerId(providerId)
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();

        socialConnectionRepository.save(connection);
    }

}
