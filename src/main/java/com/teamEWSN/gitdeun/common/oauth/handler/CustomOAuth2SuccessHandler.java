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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

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
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String state = request.getParameter("state");

        String purpose = oAuthStateService.consumeState(state); // "connect:42" 또는 null
        if (purpose != null && purpose.startsWith("connect:")) {
            Long userId = Long.parseLong(purpose.split(":")[1]);
            authService.connectGithubAccount(oAuth2User, userId);
            response.sendRedirect(frontUrl + "/oauth/callback#connected=true");
            return;
        }

        // 일반 로그인 흐름
        JwtToken jwtToken = jwtTokenProvider.generateToken(authentication);
        cookieUtil.setCookie(response, "refreshToken", jwtToken.getRefreshToken(), jwtTokenProvider.getRefreshTokenExpired());
        String targetUrl = frontUrl + "/oauth/callback#accessToken=" + jwtToken.getAccessToken();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
