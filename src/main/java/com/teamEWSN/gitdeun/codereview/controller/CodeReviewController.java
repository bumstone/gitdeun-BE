package com.teamEWSN.gitdeun.codereview.controller;


import com.teamEWSN.gitdeun.codereview.dto.CodeReviewRequestDtos.*;
import com.teamEWSN.gitdeun.codereview.dto.CodeReviewResponseDtos.*;
import com.teamEWSN.gitdeun.codereview.service.CodeReviewService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CodeReviewController {

    private final CodeReviewService codeReviewService;

    // 노드에 대한 코드 리뷰 생성
    @PostMapping(value = "/mindmaps/{mapId}/nodes/{nodeId}/code-reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponse> createNodeCodeReview(
        @PathVariable Long mapId,
        @PathVariable String nodeId,
        @Valid @RequestPart("request") CreateRequest request,
        @RequestPart(value = "files", required = false) List<MultipartFile> files,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(codeReviewService.createNodeReview(mapId, nodeId, userDetails.getId(), request, files));
    }

    // 코드 참조에 대한 코드 리뷰 생성
    @PostMapping(value = "/references/{refId}/code-reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponse> createReferenceCodeReview(
            @PathVariable Long refId,
            @Valid @RequestPart("request") CreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(codeReviewService.createReferenceReview(refId, userDetails.getId(), request, files));
    }

    // 특정 코드 리뷰 상세 정보 조회
    @GetMapping("/code-reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> getCodeReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(codeReviewService.getReview(reviewId, userDetails.getId()));
    }

    // 코드 리뷰 상태 변경 (e.g., PENDING -> RESOLVED)
    @PatchMapping("/code-reviews/{reviewId}/status")
    public ResponseEntity<Void> updateCodeReviewStatus(
            @PathVariable Long reviewId,
            @RequestBody StatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        codeReviewService.updateReviewStatus(reviewId, userDetails.getId(), request.getStatus());
        return ResponseEntity.ok().build();
    }

    // 특정 노드에 달린 코드 리뷰 목록 조회
    @GetMapping("/mindmaps/{mapId}/nodes/{nodeId}/code-reviews")
    public ResponseEntity<Page<ReviewListResponse>> getNodeCodeReviews(
            @PathVariable Long mapId,
            @PathVariable String nodeId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(codeReviewService.getReviewsForNode(mapId, nodeId, userDetails.getId(), pageable));
    }
}