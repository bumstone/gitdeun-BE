package com.teamEWSN.gitdeun.Application.controller;

import com.teamEWSN.gitdeun.Application.dto.*;
import com.teamEWSN.gitdeun.Application.service.ApplicationService;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<Page<ApplicationListResponseDto>> getMyApplications(
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
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
    public ResponseEntity<Page<ApplicationListResponseDto>> getRecruitmentApplications(
        @PathVariable Long recruitmentId,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Page<ApplicationListResponseDto> applications = applicationService.getRecruitmentApplications(
            recruitmentId, userDetails.getId(), pageable
        );
        return ResponseEntity.ok(applications);
    }

    /**
     * 지원 상세 조회
     */
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<ApplicationResponseDto> getApplication(
        @PathVariable Long applicationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ApplicationResponseDto application = applicationService.getApplication(
            applicationId, userDetails.getId()
        );
        return ResponseEntity.ok(application);
    }

    /**
     * 지원 철회 (지원자만 가능)
     */
    @PatchMapping("/applications/{applicationId}/withdraw")
    public ResponseEntity<Void> withdrawApplication(
        @PathVariable Long applicationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        applicationService.withdrawApplication(applicationId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 지원 수락 (모집자만 가능)
     */
    @PatchMapping("/applications/{applicationId}/accept")
    public ResponseEntity<ApplicationResponseDto> acceptApplication(
        @PathVariable Long applicationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ApplicationResponseDto response = applicationService.acceptApplication(
            applicationId, userDetails.getId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 지원 거절 (모집자만 가능)
     */
    @PatchMapping("/applications/{applicationId}/reject")
    public ResponseEntity<ApplicationResponseDto> rejectApplication(
        @PathVariable Long applicationId,
        @Valid @RequestBody ApplicationStatusUpdateDto updateDto,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ApplicationResponseDto response = applicationService.rejectApplication(
            applicationId, userDetails.getId(), updateDto
        );
        return ResponseEntity.ok(response);
    }

}