package com.teamEWSN.gitdeun.user.service;


import com.teamEWSN.gitdeun.common.jwt.RefreshToken;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.cookie.CookieUtil;
import com.teamEWSN.gitdeun.common.jwt.*;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final BlacklistService blacklistService;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieUtil cookieUtil;

    @Value("${jwt.refresh-expired}")
    private Long refreshTokenExpired;


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

    // connectSocialAccount 로직은 CustomOAuth2UserService의 로직과 유사하게
    // 인가 코드로 토큰을 받고, 토큰으로 유저 정보를 받아와 SocialConnection을 생성/저장해야 합니다.
    // 이 부분은 각 소셜 플랫폼의 API를 직접 호출해야 하므로 WebClient를 사용한 구현이 필요합니다.
    @Transactional
    public void connectSocialAccount(Long userId, String provider, String code) {
        // TODO: WebClient를 사용하여 provider(github)의
        // 1. 인가 코드(code)로 Access Token 요청
        // 2. Access Token으로 사용자 정보 요청
        // 3. 받아온 정보로 SocialConnection 객체 생성 및 저장
        // 이 로직은 CustomOAuth2UserService의 로직을 참고하여 작성할 수 있습니다.
        // WebClient 설정은 SecurityConfig에 Bean으로 등록 후 주입받아 사용합니다.

        System.out.printf("계정 연동 시도: userId=%d, provider=%s, code=%s\n", userId, provider, code);
    }

    // User 객체를 사용해 authentication 생성
    private Authentication createAuthentication(User user) {
        CustomUserDetails customUserDetails = new CustomUserDetails(
            user.getId(), user.getEmail(), user.getNickname(), user.getProfileImage(), user.getRole(), user.getName());

        return new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
    }
}
