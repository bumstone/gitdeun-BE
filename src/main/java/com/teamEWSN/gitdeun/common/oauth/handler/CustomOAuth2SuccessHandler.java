package com.teamEWSN.gitdeun.common.oauth.handler;

import com.teamEWSN.gitdeun.common.cookie.CookieUtil;
import com.teamEWSN.gitdeun.common.jwt.JwtToken;
import com.teamEWSN.gitdeun.common.jwt.JwtTokenProvider;
import com.teamEWSN.gitdeun.common.oauth.service.OAuthStateService;
import com.teamEWSN.gitdeun.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuthStateService oAuthStateService;
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @Value("${app.front-url}")
    private String frontUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Authentication auth) throws IOException {

        String state = req.getParameter("state");
        String purpose = state != null ? oAuthStateService.consumeState(state) : null;

        // 1) 계정 연동 시나리오
        if (purpose != null && purpose.startsWith("connect:")) {
            handleAccountConnection(purpose, (OAuth2User) auth.getPrincipal(), res);
            return;
        }

        // 2) 일반 로그인
        handleStandardLogin(req, res, auth);
    }

    /**
     * 일반 로그인 성공 시 JWT 토큰을 발급 및 클라이언트로 리디렉션
     */
    private void handleStandardLogin(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // JWT 액세스 토큰과 리프레시 토큰을 생성합니다.
        JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);

        // 리프레시 토큰은 보안을 위해 HttpOnly 쿠키에 저장합니다.
        cookieUtil.setCookie(response, "refreshToken", jwtToken.getRefreshToken(), jwtTokenProvider.getRefreshTokenExpired());

        // 액세스 토큰은 URL 프래그먼트로 프론트엔드에 전달합니다.
        String targetUrl = UriComponentsBuilder.fromUriString(frontUrl + "/oauth/callback")
            .fragment("accessToken=" + jwtToken.getAccessToken())
            .build()
            .toUriString();

        clearAuthenticationAttributes(request); // 세션 클린업
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * 기존 계정에 새로운 소셜 계정을 연동하는 흐름을 처리
     */
    private void handleAccountConnection(String purpose, OAuth2User oAuth2User, HttpServletResponse response) throws IOException {
        Long userId = Long.parseLong(purpose.split(":")[1]);
        authService.connectGithubAccount(oAuth2User, userId); // 계정 연동 로직 호출

        String targetUrl = frontUrl + "/oauth/callback#connected=true";
        response.sendRedirect(targetUrl);
    }
}
