package com.teamEWSN.gitdeun.user.controller;

import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.common.oauth.service.CustomOAuth2UserService;
import com.teamEWSN.gitdeun.user.dto.UserResponseDto;
import com.teamEWSN.gitdeun.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 개인 정보 조회
    @GetMapping
    public ResponseEntity<UserResponseDto> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        UserResponseDto userInfo = userService.getMyInfo(userId);

        return ResponseEntity.ok(userInfo);
    }


    // 현재 회원 정보를 바탕으로 회원 탈퇴
    @DeleteMapping
    public ResponseEntity<Void> deleteCurrentUser(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        @RequestHeader("Authorization") String authHeader) {
        // JwtAuthenticationFilter가 정상 동작했다면 userDetails는 절대 null이 아님
        if (refreshToken == null || refreshToken.isEmpty()) {
            // 리프레시 토큰이 없는 경우에 대한 예외 처리
            throw new GlobalException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String accessToken = authHeader.replace("Bearer ", "");

        userService.deleteUser(userDetails.getId(), accessToken, refreshToken);

        return ResponseEntity.noContent().build();
    }


}