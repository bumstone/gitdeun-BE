package com.teamEWSN.gitdeun.user.controller;

import com.teamEWSN.gitdeun.common.cookie.CookieUtil;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.common.jwt.JwtToken;
import com.teamEWSN.gitdeun.common.oauth.service.OAuthStateService;
import com.teamEWSN.gitdeun.user.dto.UserTokenResponseDto;
import com.teamEWSN.gitdeun.user.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${jwt.refresh-expired}")
    private Long refreshTokenExpired;

    private final OAuthStateService oAuthStateService;
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @Value("${app.front-url}")
    private String frontUrl;

    // 깃허브 연동 흐름 구분 조회
    @GetMapping("/connect/github/state")
    public ResponseEntity<Map<String, String>> generateStateForGithubConnect(@AuthenticationPrincipal CustomUserDetails user) {
        String state = oAuthStateService.createState("connect:" + user.getId());
        return ResponseEntity.ok(Map.of("state", state));
    }

    // 로그아웃 API
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @RequestHeader("Authorization") String authHeader,
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        // 헤더에서 Access Token 추출
        String accessToken = authHeader.replace("Bearer ", "");

        // 로그아웃 로직 - AccessToken: Blacklist 등록, RefreshToken: redis에서 삭제 및 쿠키 제거
        authService.logout(accessToken, refreshToken, response);

        return ResponseEntity.noContent().build(); // 204 No Content 응답
    }

    // 토큰 재발급
    @PostMapping("/token/refresh")
    public ResponseEntity<UserTokenResponseDto> refreshAccessToken(
        @CookieValue(name = "refreshToken") String refreshToken,
        HttpServletResponse response) {
        JwtToken newJwtToken = authService.refreshTokens(refreshToken);

        cookieUtil.setCookie(response, "refreshToken", newJwtToken.getRefreshToken(), refreshTokenExpired);
        return ResponseEntity.ok(new UserTokenResponseDto(newJwtToken.getAccessToken()));
    }

//    /**
//     * GitHub의 모든 OAuth 콜백을 처리하는 단일 엔드포인트
//     * @param code GitHub에서 제공하는 Authorization Code
//     * @param userDetails 현재 로그인된 사용자 정보. 비로그인 상태면 null.
//     * @return 로그인 또는 계정 연동 흐름에 따라 적절한 경로로 포워딩 또는 리디렉션
//     */
//    @GetMapping("/github/callback")
//    public ResponseEntity<?> githubCallback(
//        @RequestParam("code") String code,
//        @AuthenticationPrincipal CustomUserDetails userDetails,
//        HttpServletResponse response // 쿠키 설정을 위해 필요
//    ) {
//
//        if (userDetails != null) {
//            // "계정 연동" 흐름
//            authService.connectGithubAccount(userDetails.getId(), code);
//            // 성공했다는 응답 전달
//            return ResponseEntity.ok().body(Map.of("status", "success", "message", "계정 연동 성공!"));
//
//        } else {
//            // "최초 로그인" 흐름
//            GithubLoginResponseDto loginResponse = authService.loginWithGithub(code, response);
//            return ResponseEntity.ok(loginResponse);
//        }
//    }

}
