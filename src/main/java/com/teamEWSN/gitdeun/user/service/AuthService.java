package com.teamEWSN.gitdeun.user.service;


import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.cookie.CookieUtil;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieUtil cookieUtil;

    @Value("${jwt.refresh-expired}")
    private Long refreshTokenExpired;

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION).replace("Bearer ", "");

        // 1. Redis에서 Refresh Token 삭제
        refreshTokenRepository.findByAccessToken(accessToken)
            .ifPresent(refreshTokenRepository::delete);

        // 2. 쿠키에서 Refresh Token 삭제
        cookieUtil.deleteCookie(response, "refreshToken");
    }

    @Transactional
    public JwtToken refreshTokens(String oldRefreshToken) {
        // 1. Redis에서 Refresh Token 조회 및 유효성 검증
        RefreshToken refreshToken = refreshTokenRepository.findById(oldRefreshToken)
            .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 2. 새로운 토큰 생성
        JwtToken newJwtToken = jwtTokenProvider.generateToken(refreshToken.getUserId(), refreshToken.getRole());

        // 3. 기존 Refresh Token을 삭제하고 새로 발급된 토큰 정보로 저장
        refreshTokenRepository.delete(refreshToken);
        refreshTokenRepository.save(new RefreshToken(newJwtToken.getRefreshToken(), refreshToken.getUserId(), refreshToken.getRole(), newJwtToken.getAccessToken()));

        return newJwtToken;
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


}
