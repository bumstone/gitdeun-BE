package com.teamEWSN.gitdeun.repo.controller;

import com.teamEWSN.gitdeun.repo.dto.RepoResponseDto;
import com.teamEWSN.gitdeun.repo.dto.RepoUpdateCheckResponseDto;
import com.teamEWSN.gitdeun.repo.service.RepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repos")
public class RepoController {

    private final RepoService repoService;

    // 리포지토리 URL을 통한 등록 확인
    @GetMapping("/check")
    public ResponseEntity<RepoResponseDto> checkRepoExists(@RequestParam String url) {
        RepoResponseDto response = repoService.findRepoByUrl(url);
        return ResponseEntity.ok(response);
    }

    // 리포지토리 정보 조회
    @GetMapping("/{repoId}")
    public ResponseEntity<RepoResponseDto> getRepoInfo(@PathVariable Long repoId) {
        RepoResponseDto response = repoService.findRepoById(repoId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 리포지토리에 대한 업데이트 필요 여부 확인
     */
    @GetMapping("/{repoId}/status")
    public ResponseEntity<RepoUpdateCheckResponseDto> getRepoUpdateStatus(@PathVariable Long repoId) {
        RepoUpdateCheckResponseDto response = repoService.checkUpdateNeeded(repoId);
        return ResponseEntity.ok(response);
    }


}