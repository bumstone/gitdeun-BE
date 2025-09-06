package com.teamEWSN.gitdeun.Application.controller;

import com.teamEWSN.gitdeun.Application.dto.*;
import com.teamEWSN.gitdeun.Application.service.ApplicationService;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Application", description = "모집 공고 지원 관련 API")
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * 모집 공고에 지원하기
     */
    @PostMapping("/recruitments/{recruitmentId}/applications")
    @Operation(summary = "모집 공고 지원", description = "특정 모집 공고에 지원합니다.")
    public ResponseEntity<ApplicationResponseDto> createApplication(
        @PathVariable Long recruitmentId,
        @Valid @RequestBody ApplicationCreateRequestDto requestDto,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Creating application for recruitment: {} by user: {}", recruitmentId, userDetails.getId());
        ApplicationResponseDto response = applicationService.createApplication(
            recruitmentId, requestDto, userDetails.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 지원 목록 조회
     */
    @GetMapping("/users/me/applications")
    @Operation(summary = "내 지원 목록 조회", description = "로그인한 사용자의 지원 목록을 조회합니다.")
    public ResponseEntity<Page<ApplicationListResponseDto>> getMyApplications(
        @PageableDefault(size = 10, sort = "createdAt,desc") Pageable pageable,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Getting applications for user: {}", userDetails.getId());
        Page<ApplicationListResponseDto> applications = applicationService.getMyApplications(
            userDetails.getId(), pageable
        );
        return ResponseEntity.ok(applications);
    }

    /**
     * 특정 공고의 지원자 목록 조회 (모집자만 가능)
     */
    @GetMapping("/recruitments/{recruitmentId}/applications")
    @Operation(summary = "공고 지원자 목록 조회", description = "모집자가 자신의 공고에 지원한 지원자 목록을 조회합니다.")
    public ResponseEntity<Page<ApplicationListResponseDto>> getRecruitmentApplications(
        @PathVariable Long recruitmentId,
        @PageableDefault(size = 10, sort = "createdAt,desc") Pageable pageable,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Getting applications for recruitment: {} by user: {}", recruitmentId, userDetails.getId());
        Page<ApplicationListResponseDto> applications = applicationService.getRecruitmentApplications(
            recruitmentId, userDetails.getId(), pageable
        );
        return ResponseEntity.ok(applications);
    }

    /**
     * 지원 상세 조회
     */
    @GetMapping("/applications/{applicationId}")
    @Operation(summary = "지원 상세 조회", description = "특정 지원의 상세 정보를 조회합니다.")
    public ResponseEntity<ApplicationResponseDto> getApplication(
        @PathVariable Long applicationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Getting application: {} by user: {}", applicationId, userDetails.getId());
        ApplicationResponseDto application = applicationService.getApplication(
            applicationId, userDetails.getId()
        );
        return ResponseEntity.ok(application);
    }

    /**
     * 지원 철회 (지원자만 가능)
     */
    @PatchMapping("/applications/{applicationId}/withdraw")
    @Operation(summary = "지원 철회", description = "본인의 지원을 철회합니다.")
    public ResponseEntity<Void> withdrawApplication(
        @PathVariable Long applicationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Withdrawing application: {} by user: {}", applicationId, userDetails.getId());
        applicationService.withdrawApplication(applicationId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 지원 수락 (모집자만 가능)
     */
    @PatchMapping("/applications/{applicationId}/accept")
    @Operation(summary = "지원 수락", description = "모집자가 지원을 수락합니다.")
    public ResponseEntity<ApplicationResponseDto> acceptApplication(
        @PathVariable Long applicationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Accepting application: {} by recruiter: {}", applicationId, userDetails.getId());
        ApplicationResponseDto response = applicationService.acceptApplication(
            applicationId, userDetails.getId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 지원 거절 (모집자만 가능)
     */
    @PatchMapping("/applications/{applicationId}/reject")
    @Operation(summary = "지원 거절", description = "모집자가 지원을 거절합니다.")
    public ResponseEntity<ApplicationResponseDto> rejectApplication(
        @PathVariable Long applicationId,
        @Valid @RequestBody ApplicationStatusUpdateDto updateDto,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("Rejecting application: {} by recruiter: {}", applicationId, userDetails.getId());
        ApplicationResponseDto response = applicationService.rejectApplication(
            applicationId, userDetails.getId(), updateDto
        );
        return ResponseEntity.ok(response);
    }

}