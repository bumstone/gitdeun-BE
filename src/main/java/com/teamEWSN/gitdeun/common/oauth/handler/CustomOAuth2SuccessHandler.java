package com.teamEWSN.gitdeun.common.oauth.handler;

import com.teamEWSN.gitdeun.common.cookie.CookieUtil;
import com.teamEWSN.gitdeun.common.oauth.entity.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    @Value("${app.front-url}")
    private String frontUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // 우리 서비스의 JWT 생성
        JwtToken jwtToken = jwtTokenProvider.generateToken(oAuth2User.getUserId(), oAuth2User.getRole());
        log.info("JWT가 발급되었습니다. Access Token: {}", jwtToken.getAccessToken());

        // Refresh Token은 HttpOnly 쿠키에 저장
        cookieUtil.setCookie(response, "refreshToken", jwtToken.getRefreshToken(), jwtTokenProvider.getRefreshTokenExpiredSeconds());

        // Access Token은 쿼리 파라미터로 프론트엔드에 전달
        String targetUrl = UriComponentsBuilder.fromUriString(frontUrl + "/oauth/callback")
            .queryParam("accessToken", jwtToken.getAccessToken())
            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
