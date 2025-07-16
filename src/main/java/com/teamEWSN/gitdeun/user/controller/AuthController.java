package com.teamEWSN.gitdeun.user.controller;

import com.teamEWSN.gitdeun.common.cookie.CookieUtil;
import com.teamEWSN.gitdeun.user.dto.UserTokenResponseDto;
import com.teamEWSN.gitdeun.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    // 토큰 재발급
    @PostMapping("/token/refresh")
    public ResponseEntity<UserTokenResponseDto> refreshAccessToken(
        @CookieValue(name = "refreshToken") String refreshToken,
        HttpServletResponse response) {
        JwtToken newJwtToken = authService.refreshTokens(refreshToken);

        cookieUtil.setCookie(response, "refreshToken", newJwtToken.getRefreshToken(), authService.getRefreshTokenExpiredSeconds());
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
