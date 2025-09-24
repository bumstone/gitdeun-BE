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

    @PostMapping("/nodes/{nodeKey}/code-references")
    public ResponseEntity<ReferenceResponse> createCodeReference(
            @PathVariable Long mapId,
            @PathVariable String nodeKey,
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(codeReferenceService.createReference(mapId, nodeKey, userDetails.getId(), request, authorizationHeader));
    }

    @GetMapping("/code-references/{refId}/detail")
    public ResponseEntity<ReferenceDetailResponse> getCodeReferenceDetail(
            @PathVariable Long mapId,
            @PathVariable Long refId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(
                codeReferenceService.getReferenceDetail(mapId, refId, userDetails.getId(), authorizationHeader)
        );
    }

    @GetMapping("/nodes/{nodeKey}/code-references")
    public ResponseEntity<List<ReferenceResponse>> getNodeCodeReferences(
            @PathVariable Long mapId,
            @PathVariable String nodeKey,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                codeReferenceService.getReferencesForNode(mapId, nodeKey, userDetails.getId())
        );
    }

    @PatchMapping("/code-references/{refId}")
    public ResponseEntity<ReferenceResponse> updateCodeReference(
            @PathVariable Long mapId,
            @PathVariable Long refId,
            @Valid @RequestBody CreateRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                codeReferenceService.updateReference(mapId, refId, userDetails.getId(), request, authorizationHeader)
        );
    }

    @DeleteMapping("/code-references/{refId}")
    public ResponseEntity<Void> deleteCodeReference(
            @PathVariable Long mapId,
            @PathVariable Long refId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        codeReferenceService.deleteReference(mapId, refId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
