package com.teamEWSN.gitdeun.visithistory.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.visithistory.service.PinnedHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/history/{historyId}/mindmaps/pinned")
@RequiredArgsConstructor
public class PinnedHistoryController {

    private final PinnedHistoryService pinnedHistoryService;

    // 핀 고정
    @PostMapping
    public ResponseEntity<Void> fixPinned(
        @PathVariable("historyId") Long historyId,
        @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        pinnedHistoryService.fixPinned(historyId, customUserDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 핀 해제
    @DeleteMapping
    public ResponseEntity<Void> removePinned(
        @PathVariable("historyId") Long historyId,
        @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        pinnedHistoryService.removePinned(historyId, customUserDetails.getId());
        return ResponseEntity.noContent().build();
    }

}
