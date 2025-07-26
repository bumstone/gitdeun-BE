package com.teamEWSN.gitdeun.user.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.user.dto.UserSettingResponseDto;
import com.teamEWSN.gitdeun.user.dto.UserSettingUpdateRequestDto;
import com.teamEWSN.gitdeun.user.service.UserSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class UserSettingController {
    private final UserSettingService userSettingService;

    /**
     * 현재 로그인된 사용자의 설정을 조회합니다.
     * @param userDetails 인증된 사용자 정보
     * @return 현재 설정 정보를 담은 응답
     */
    @GetMapping
    public ResponseEntity<UserSettingResponseDto> getUserSettings(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserSettingResponseDto responseDto = userSettingService.getSettings(userDetails.getId());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 현재 로그인된 사용자의 설정을 변경합니다.
     * @param userDetails 인증된 사용자 정보
     * @param requestDto 변경할 설정 정보를 담은 요청 DTO
     * @return 변경된 설정 정보를 담은 응답
     */
    @PatchMapping // 리소스의 일부만 변경하므로 PATCH가 더 적합합니다.
    public ResponseEntity<UserSettingResponseDto> updateUserSettings(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UserSettingUpdateRequestDto requestDto
    ) {
        UserSettingResponseDto responseDto = userSettingService.updateSettings(userDetails.getId(), requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
