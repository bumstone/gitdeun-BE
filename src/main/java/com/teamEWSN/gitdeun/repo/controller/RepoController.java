package com.teamEWSN.gitdeun.repo.controller;

import com.teamEWSN.gitdeun.repo.dto.RepoResponseDto;
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
        return repoService.findRepoByUrl(url) // Optional<RepoResponseDto>를 받음
            .map(ResponseEntity::ok) // 값이 있으면 200 OK와 함께 body에 담아 반환
            .orElseGet(() -> ResponseEntity.noContent().build()); // 값이 없으면 204 No Content 반환
    }

    // 리포지토리 정보 조회
    @GetMapping("/{repoId}")
    public ResponseEntity<RepoResponseDto> getRepoInfo(@PathVariable Long repoId) {
        RepoResponseDto response = repoService.findRepoById(repoId);
        return ResponseEntity.ok(response);
    }


}