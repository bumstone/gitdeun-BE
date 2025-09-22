package com.teamEWSN.gitdeun.codereference.controller;

import com.teamEWSN.gitdeun.codereference.dto.CodeReferenceResponseDtos.*;
import com.teamEWSN.gitdeun.codereference.dto.*;
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
    @PostMapping("/nodes/{nodeId}/code-references")
    public ResponseEntity<ReferenceResponse> createCodeReference(
        @PathVariable Long mapId,
        @PathVariable String nodeId,
        @Valid @RequestBody CodeReferenceRequestDtos.CreateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(codeReferenceService.createReference(mapId, nodeId, userDetails.getId(), request));
    }

    // 특정 코드 참조 상세 조회
    @GetMapping("/code-references/{refId}")
    public ResponseEntity<ReferenceResponse> getCodeReference(
        @PathVariable Long mapId,
        @PathVariable Long refId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(codeReferenceService.getReference(mapId, refId, userDetails.getId()));
    }

    // 특정 노드에 연결된 모든 코드 참조 목록 조회
    @GetMapping("/nodes/{nodeId}/code-references")
    public ResponseEntity<List<ReferenceResponse>> getNodeCodeReferences(
        @PathVariable Long mapId,
        @PathVariable String nodeId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(codeReferenceService.getReferencesForNode(mapId, nodeId, userDetails.getId()));
    }

    // 코드 참조 정보 수정
    @PatchMapping("/code-references/{refId}")
    public ResponseEntity<ReferenceResponse> updateCodeReference(
        @PathVariable Long mapId,
        @PathVariable Long refId,
        @Valid @RequestBody CodeReferenceRequestDtos.CreateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(codeReferenceService.updateReference(mapId, refId, userDetails.getId(), request));
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
