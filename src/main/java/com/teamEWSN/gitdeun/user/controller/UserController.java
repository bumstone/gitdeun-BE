package com.teamEWSN.gitdeun.user.controller;

import com.teamEWSN.gitdeun.common.oauth.service.CustomOAuth2UserService;
import com.teamEWSN.gitdeun.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CustomOAuth2UserService customOAuth2UserService;

    // 개인 정보 조회
    @GetMapping
    public ResponseEntity<UserResponseDto> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        UserResponseDto userInfo = userService.getMyInfo(userId);

        return ResponseEntity.ok(userInfo);
    }


    // 현재 회원 정보를 바탕으로 회원 탈퇴
    @DeleteMapping
    public ResponseEntity<Void> deleteCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deleteUser(userDetails.getId());
        return ResponseEntity.noContent().build();
    }


}