package com.teamEWSN.gitdeun.Recruitment.controller;

import com.teamEWSN.gitdeun.Recruitment.dto.*;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentStatus;
import com.teamEWSN.gitdeun.Recruitment.service.RecruitmentService;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    /**
     * 신규 모집 공고 생성 API
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param requestDto  생성할 공고 정보
     * @return 생성된 공고의 상세 정보
     */
    @PostMapping(value = "/recruitments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecruitmentDetailResponseDto> createRecruitment(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute RecruitmentCreateRequestDto requestDto
    ) {
        RecruitmentDetailResponseDto responseDto = recruitmentService.createRecruitment(userDetails.getId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * 내가 작성한 모집 공고 목록 조회 API
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param pageable    페이징 정보
     * @return 내 모집 공고 목록
     */
    @GetMapping("/users/me/recruitments")
    public ResponseEntity<Page<RecruitmentListResponseDto>> getMyRecruitments(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<RecruitmentListResponseDto> response = recruitmentService.getMyRecruitments(userDetails.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 모집 공고 상세 조회 API
     *
     * @param recruitmentId 조회할 공고 ID
     * @return 공고 상세 정보
     */
    @GetMapping("/recruitments/{id}")
    public ResponseEntity<RecruitmentDetailResponseDto> getRecruitment(@PathVariable("id") Long recruitmentId) {
        RecruitmentDetailResponseDto responseDto = recruitmentService.getRecruitment(recruitmentId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 모집 공고 목록 필터링 조회 API
     *
     * @param status   모집 상태 (FORTHCOMING, RECRUITING, CLOSED, COMPLETED)
     * @param field    모집 분야 (BACKEND, FRONTEND 등)
     * @param pageable 페이징 정보
     * @return 필터링된 공고 목록
     */
    @GetMapping("/recruitments")
    public ResponseEntity<Page<RecruitmentListResponseDto>> searchRecruitments(
        @RequestParam(required = false) RecruitmentStatus status,
        @RequestParam(required = false) List<RecruitmentField> field,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<RecruitmentListResponseDto> response = recruitmentService.searchRecruitments(status, field, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 모집 공고 수정 API (작성자만 가능)
     *
     * @param recruitmentId 수정할 공고 ID
     * @param userDetails   현재 로그인한 사용자 정보
     * @param requestDto    수정할 공고 정보
     * @return 수정된 공고 상세 정보
     */
    @PutMapping(value = "/recruitments/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecruitmentDetailResponseDto> updateRecruitment(
        @PathVariable("id") Long recruitmentId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @ModelAttribute RecruitmentUpdateRequestDto requestDto
    ) {
        RecruitmentDetailResponseDto responseDto = recruitmentService.updateRecruitment(recruitmentId, userDetails.getId(), requestDto);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 모집 공고 삭제 API (작성자만 가능)
     *
     * @param recruitmentId 삭제할 공고 ID
     * @param userDetails   현재 로그인한 사용자 정보
     * @return 응답 없음 (204 No Content)
     */
    @DeleteMapping("/recruitments/{id}")
    public ResponseEntity<Void> deleteRecruitment(
        @PathVariable("id") Long recruitmentId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        recruitmentService.deleteRecruitment(recruitmentId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 맞춤 추천 공고 목록 조회 API (가중치 기반)
     *
     * @param userDetails 현재 로그인한 사용자 정보
     * @param pageable    페이징 정보
     * @return 추천 공고 목록
     */
    @GetMapping("/recruitments/recommendations")
    public ResponseEntity<Page<RecruitmentListResponseDto>> getRecommendedRecruitments(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<RecruitmentListResponseDto> response = recruitmentService.getRecommendedRecruitments(userDetails.getId(), pageable);
        return ResponseEntity.ok(response);
    }
}
