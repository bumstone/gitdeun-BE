package com.teamEWSN.gitdeun.codereference.controller;

import com.teamEWSN.gitdeun.codereference.dto.CodeReferenceResponseDtos.*;
import com.teamEWSN.gitdeun.codereference.dto.CodeReferenceRequestDtos.*;
import com.teamEWSN.gitdeun.codereference.service.CodeReferenceService;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mindmaps/{mapId}")
@RequiredArgsConstructor
public class CodeReferenceController {

    private final CodeReferenceService codeReferenceService;

    // 특정 노드에 코드 참조 생성
    @PostMapping("/nodes/{nodeKey}/code-references")
    public ResponseEntity<ReferenceResponse> createCodeReference(
        @PathVariable Long mapId,
        @PathVariable String nodeKey,
        @Valid @RequestBody CreateRequest request,
        @RequestHeader("Authorization") String authorizationHeader,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(codeReferenceService.createReference(mapId, nodeKey, userDetails.getId(), request, authorizationHeader));
    }

    // 특정 코드 참조 상세 조회
    @GetMapping("/code-references/{refId}/detail")
    public ResponseEntity<ReferenceDetailResponse> getCodeReferenceDetail(
        @PathVariable Long mapId,
        @PathVariable Long refId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(codeReferenceService.getReferenceDetail(mapId, refId, userDetails.getId(), authorizationHeader));
    }

    // 특정 노드에 연결된 모든 코드 참조 목록 조회
    @GetMapping("/nodes/{nodeKey}/code-references")
    public ResponseEntity<List<ReferenceResponse>> getNodeCodeReferences(
        @PathVariable Long mapId,
        @PathVariable String nodeKey,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(codeReferenceService.getReferencesForNode(mapId, nodeKey, userDetails.getId()));
    }

    // 코드 참조 정보 수정
    @PatchMapping("/code-references/{refId}")
    public ResponseEntity<ReferenceResponse> updateCodeReference(
        @PathVariable Long mapId,
        @PathVariable Long refId,
        @Valid @RequestBody CreateRequest request,
        @RequestHeader("Authorization") String authorizationHeader,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(codeReferenceService.updateReference(mapId, refId, userDetails.getId(), request, authorizationHeader));
    }

    // 코드 참조 삭제
    @DeleteMapping("/code-references/{refId}")
    public ResponseEntity<Void> deleteCodeReference(
        @PathVariable Long mapId,
        @PathVariable Long refId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        codeReferenceService.deleteReference(mapId, refId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
