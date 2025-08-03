package com.teamEWSN.gitdeun.repo.service;

import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.repo.dto.RepoResponseDto;
import com.teamEWSN.gitdeun.repo.dto.RepoUpdateCheckResponseDto;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import com.teamEWSN.gitdeun.repo.mapper.RepoMapper;
import com.teamEWSN.gitdeun.repo.repository.RepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로는 읽기 전용, 데이터 변경 메서드에 @Transactional을 별도 추가
public class RepoService {

    private final RepoRepository repoRepository;
    private final RepoMapper repoMapper;
    private final FastApiClient fastApiClient;

    // 레포지토리 ID로 정보 조회
    public RepoResponseDto findRepoById(Long repoId) {
        Repo repo = repoRepository.findById(repoId)
            .orElseThrow(() -> new GlobalException(ErrorCode.REPO_NOT_FOUND_BY_ID));
        return repoMapper.toResponseDto(repo);
    }

    // 리포지토리 URL을 통한 조회하여 등록 확인
    public Optional<RepoResponseDto> findRepoByUrl(String url) {
        return repoRepository.findByGithubRepoUrl(url)
            .map(repoMapper::toResponseDto);
    }

    /**
     * 리포지토리의 최신 업데이트 상태를 실시간으로 확인
     * @param repoId 확인할 리포지토리의 ID
     * @return 업데이트 필요 여부를 담은 DTO
     */
    public RepoUpdateCheckResponseDto checkUpdateNeeded(Long repoId) {
        // 시스템의 마지막 동기화 시간 조회
        Repo repo = repoRepository.findById(repoId)
            .orElseThrow(() -> new GlobalException(ErrorCode.REPO_NOT_FOUND_BY_ID));
        LocalDateTime lastSyncedAt = repo.getGithubLastUpdatedAt();

        // FastAPI의 가벼운 API를 호출하여 GitHub의 최신 커밋 시간 조회
        LocalDateTime latestCommitAt = fastApiClient.fetchLatestCommitTime(repo.getGithubRepoUrl());

        // 두 시간을 비교하여 업데이트 필요 여부 결정
        boolean isNeeded = latestCommitAt.isAfter(lastSyncedAt);

        return new RepoUpdateCheckResponseDto(isNeeded);
    }

    // 마인드맵 생성 시 repo 생성 및 업데이트
    @Transactional
    public Repo createOrUpdate(String repoUrl, AnalysisResultDto dto) {
        return repoRepository.findByGithubRepoUrl(repoUrl)
            .map(r -> { r.updateWithAnalysis(dto); return r; })
            .orElseGet(() -> {
                Repo repo = Repo.builder()
                    .githubRepoUrl(repoUrl)
                    .build();
                repo.updateWithAnalysis(dto);
                return repo;
            });
    }
}