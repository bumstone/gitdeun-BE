package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.webhook.dto.WebhookUpdateDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapCreateRequestDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapDetailResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapResponseDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapMember;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import com.teamEWSN.gitdeun.mindmap.entity.MindmapType;
import com.teamEWSN.gitdeun.mindmap.mapper.MindmapMapper;
import com.teamEWSN.gitdeun.mindmapmember.repository.MindmapMemberRepository;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import com.teamEWSN.gitdeun.repo.repository.RepoRepository;
import com.teamEWSN.gitdeun.repo.service.RepoService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.visithistory.service.VisitHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapService {

    private final VisitHistoryService visitHistoryService;
    private final RepoService repoService;
    private final MindmapSseService mindmapSseService;
    private final MindmapAuthService mindmapAuthService;
    private final MindmapMapper mindmapMapper;
    private final MindmapRepository mindmapRepository;
    private final MindmapMemberRepository mindmapMemberRepository;
    private final RepoRepository repoRepository;
    private final UserRepository userRepository;
    private final FastApiClient fastApiClient;

    @Transactional
    public MindmapResponseDto createMindmapFromAnalysis(MindmapCreateRequestDto req, AnalysisResultDto dto, Long userId, String authorizationHeader) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Repo repo = repoService.createOrUpdate(req.getRepoUrl(), dto);
        repoRepository.save(repo);

        String field = determineField(req, user);

        // 1. ArangoDB에 초기 마인드맵 데이터를 저장하고 키를 받아옴
        String arangodbKey = null;
        String finalMapData = dto.getMapData();

        try {
            // FastAPI를 통해 ArangoDB에 데이터 저장
            arangodbKey = fastApiClient.saveArangoData(
                req.getRepoUrl(),
                dto.getMapData(),
                authorizationHeader
            );

            // ArangoDB에서 저장된 데이터 조회하여 최종 mapData 확정
            var arangoData = fastApiClient.getArangoData(arangodbKey, authorizationHeader);
            if (arangoData != null && arangoData.getMapData() != null) {
                finalMapData = arangoData.getMapData();
            }

        } catch (Exception e) {
            log.warn("ArangoDB 저장 중 오류 발생, 기본 데이터로 진행: {}", e.getMessage());
            // ArangoDB 저장 실패 시에도 마인드맵은 생성하되, arangodbKey는 null로 유지
        }

        // 2. MySQL에 마인드맵 엔티티 저장
        Mindmap mindmap = Mindmap.builder()
            .repo(repo)
            .user(user)
            .prompt(req.getPrompt())
            .branch(dto.getDefaultBranch())
            .type(req.getType())
            .field(field)
            .mapData(finalMapData) // ArangoDB에서 가져온 최종 데이터
            .arangodbKey(arangodbKey) // ArangoDB 키 저장
            .build();

        mindmapRepository.save(mindmap);

        // 3. 마인드맵 소유자 등록
        mindmapMemberRepository.save(
            MindmapMember.of(mindmap, user, MindmapRole.OWNER)
        );

        // 4. 방문 기록 생성
        visitHistoryService.createVisitHistory(user, mindmap);

        return mindmapMapper.toResponseDto(mindmap);
    }

    private String determineField(MindmapCreateRequestDto req, User user) {
        if (req.getType() == MindmapType.DEV) {
            return "개발용";
        } else {
            if (req.getField() != null && !req.getField().isEmpty()) {
                return req.getField();
            } else {
                long nextSeq = findNextCheckSequence(user);
                return "확인용 (" + nextSeq + ")";
            }
        }
    }

    /**
     * 특정 사용자의 "확인용 (n)" 다음 시퀀스 번호를 찾습니다.
     */
    private long findNextCheckSequence(User user) {
        Optional<Mindmap> lastCheckMindmap = mindmapRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user);

        if (lastCheckMindmap.isEmpty()) {
            return 1;
        }

        Pattern pattern = Pattern.compile("\\((\\d+)\\)");
        Matcher matcher = pattern.matcher(lastCheckMindmap.get().getField());

        if (matcher.find()) {
            long lastSeq = Long.parseLong(matcher.group(1));
            return lastSeq + 1;
        }

        return 1;
    }

    /**
     * 마인드맵 상세 정보 조회 - ArangoDB와 동기화된 최신 데이터 반환
     */
    @Transactional
    public MindmapDetailResponseDto getMindmap(Long mapId, Long userId, String authorizationHeader) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        // ArangoDB에서 최신 데이터 동기화
        syncWithArangoDB(mindmap, authorizationHeader);

        return mindmapMapper.toDetailResponseDto(mindmap);
    }

    /**
     * 마인드맵 새로고침 - ArangoDB와 완전 동기화
     */
    @Transactional
    public MindmapDetailResponseDto refreshMindmap(Long mapId, Long userId, String authorizationHeader) {
        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        // 마인드맵 생성자만 새로고침 가능
        if (!mindmap.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        try {
            // 1. FastAPI를 통해 리포지토리 재분석
            AnalysisResultDto dto = fastApiClient.analyze(
                mindmap.getRepo().getGithubRepoUrl(),
                mindmap.getPrompt(),
                mindmap.getType(),
                authorizationHeader
            );

            // 2. 리포지토리 정보 업데이트
            mindmap.getRepo().updateWithAnalysis(dto);

            // 3. ArangoDB 데이터 업데이트 또는 신규 생성
            String finalMapData = dto.getMapData();

            if (mindmap.getArangodbKey() != null) {
                // 기존 ArangoDB 데이터 업데이트
                var arangoData = fastApiClient.updateArangoData(
                    mindmap.getArangodbKey(),
                    dto.getMapData(),
                    authorizationHeader
                );
                if (arangoData != null && arangoData.getMapData() != null) {
                    finalMapData = arangoData.getMapData();
                }
            } else {
                // ArangoDB 키가 없다면 신규 생성
                String newArangodbKey = fastApiClient.saveArangoData(
                    mindmap.getRepo().getGithubRepoUrl(),
                    dto.getMapData(),
                    authorizationHeader
                );
                mindmap.updateArangodbKey(newArangodbKey); // Mindmap 엔티티에 이 메소드 추가 필요

                // 저장된 데이터 조회
                var arangoData = fastApiClient.getArangoData(newArangodbKey, authorizationHeader);
                if (arangoData != null && arangoData.getMapData() != null) {
                    finalMapData = arangoData.getMapData();
                }
            }

            // 4. MySQL 마인드맵 데이터 업데이트
            mindmap.updateMapData(finalMapData);

        } catch (Exception e) {
            log.error("새로고침 중 ArangoDB 연동 실패: {}", e.getMessage());
            // ArangoDB 연동 실패 시에도 기본 FastAPI 결과로 업데이트
            AnalysisResultDto dto = fastApiClient.analyze(
                mindmap.getRepo().getGithubRepoUrl(),
                mindmap.getPrompt(),
                mindmap.getType(),
                authorizationHeader
            );
            mindmap.getRepo().updateWithAnalysis(dto);
            mindmap.updateMapData(dto.getMapData());
        }

        MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap);

        // 업데이트된 마인드맵 정보를 모든 구독자에게 방송
        mindmapSseService.broadcastUpdate(mapId, responseDto);

        return responseDto;
    }

    /**
     * ArangoDB와 동기화하여 최신 마인드맵 데이터를 가져옴
     */
    private void syncWithArangoDB(Mindmap mindmap, String authorizationHeader) {
        if (mindmap.getArangodbKey() == null) {
            return; // ArangoDB 키가 없으면 동기화 불가
        }

        try {
            var arangoData = fastApiClient.getArangoData(
                mindmap.getArangodbKey(),
                authorizationHeader
            );

            if (arangoData != null && arangoData.getMapData() != null) {
                // ArangoDB 데이터가 MySQL 데이터와 다르면 업데이트
                if (!arangoData.getMapData().equals(mindmap.getMapData())) {
                    mindmap.updateMapData(arangoData.getMapData());
                    log.info("마인드맵 ID {}의 데이터가 ArangoDB와 동기화되었습니다", mindmap.getId());
                }
            }
        } catch (Exception e) {
            log.warn("ArangoDB 동기화 실패, 기존 데이터 유지: {}", e.getMessage());
        }
    }

    // webhook을 통한 업데이트
    @Transactional
    public void updateMindmapFromWebhook(WebhookUpdateDto dto, String authorizationHeader) {
        Repo repo = repoRepository.findByGithubRepoUrl(dto.getRepoUrl())
            .orElseThrow(() -> new GlobalException(ErrorCode.REPO_NOT_FOUND_BY_URL));

        List<Mindmap> mindmapsToUpdate = repo.getMindmaps();

        // Repo 정보 업데이트
        repo.updateWithWebhookData(dto);

        // 각 마인드맵의 ArangoDB 데이터와 MySQL 데이터 동기화
        for (Mindmap mindmap : mindmapsToUpdate) {
            try {
                // Webhook 데이터를 ArangoDB에 업데이트
                if (mindmap.getArangodbKey() != null) {
                    var arangoData = fastApiClient.updateArangoData(
                        mindmap.getArangodbKey(),
                        dto.getMapData(),
                        authorizationHeader
                    );

                    // ArangoDB에서 반환된 데이터로 MySQL 업데이트
                    if (arangoData != null && arangoData.getMapData() != null) {
                        mindmap.updateMapData(arangoData.getMapData());
                    } else {
                        mindmap.updateMapData(dto.getMapData());
                    }
                } else {
                    // ArangoDB 키가 없으면 직접 업데이트
                    mindmap.updateMapData(dto.getMapData());
                }
            } catch (Exception e) {
                log.warn("Webhook 처리 중 ArangoDB 연동 실패, 직접 업데이트: {}", e.getMessage());
                mindmap.updateMapData(dto.getMapData());
            }

            MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap);
            mindmapSseService.broadcastUpdate(mindmap.getId(), responseDto);

            log.info("Webhook으로 마인드맵 ID {} 업데이트 및 SSE 전송 완료", mindmap.getId());
        }
    }

    /**
     * 마인드맵 삭제 - ArangoDB 데이터도 함께 삭제
     */
    @Transactional
    public void deleteMindmap(Long mapId, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        // 마인드맵 생성자만 삭제 가능
        if (!mindmap.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // ArangoDB 데이터 삭제
        if (mindmap.getArangodbKey() != null) {
            try {
                fastApiClient.deleteAnalysisData(mindmap.getArangodbKey());
                log.info("ArangoDB에서 마인드맵 데이터 삭제 완료: {}", mindmap.getArangodbKey());
            } catch (Exception e) {
                log.error("ArangoDB 데이터 삭제 실패하지만 MySQL 삭제는 진행: {}", e.getMessage());
            }
        }

        // MySQL에서 마인드맵 삭제
        mindmapRepository.delete(mindmap);
        log.info("마인드맵 ID {} 삭제 완료", mapId);
    }

}