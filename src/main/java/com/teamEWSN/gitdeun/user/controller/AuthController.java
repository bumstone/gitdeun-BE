package com.teamEWSN.gitdeun.user.controller;

import com.teamEWSN.gitdeun.common.cookie.CookieUtil;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.common.jwt.JwtToken;
import com.teamEWSN.gitdeun.user.dto.UserTokenResponseDto;
import com.teamEWSN.gitdeun.user.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${jwt.refresh-expired}")
    private Long refreshTokenExpired;

    private final AuthService authService;
    private final CookieUtil cookieUtil;


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

    /**
     * 이미 로그인된 사용자의 계정에 추가로 소셜 계정을 연동
     * @param userDetails 현재 로그인된 사용자 정보 (JWT 기반)
     * @param provider 연동할 소셜 플랫폼
     * @param code 연동할 계정의 인가 코드
     */
    @GetMapping("/connect/{provider}")
    public ResponseEntity<Void> connectSocialAccount(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable("provider") String provider,
        @RequestParam String code) {

        authService.connectSocialAccount(userDetails.getId(), provider, code);
        return ResponseEntity.ok().build();
    }

}
