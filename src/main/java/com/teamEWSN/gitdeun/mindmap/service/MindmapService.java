package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
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
    public MindmapResponseDto createMindmapFromAnalysis(MindmapCreateRequestDto req, AnalysisResultDto dto, Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Repo repo = repoService.createOrUpdate(req.getRepoUrl(), dto);
        repoRepository.save(repo);

        String field;
        if (req.getType() == MindmapType.DEV) {
            field = "개발용";
        } else {
            if (req.getField() != null && !req.getField().isEmpty()) {
                field = req.getField();
            } else {
                // findNextCheckSequence 호출 시 repo 정보 제거
                long nextSeq = findNextCheckSequence(user);
                field = "확인용 (" + nextSeq + ")";
            }
        }

        Mindmap mindmap = Mindmap.builder()
            .repo(repo)
            .user(user)
            .prompt(req.getPrompt())
            .branch(dto.getDefaultBranch())
            .type(req.getType())
            .field(field)
            .mapData(dto.getMapData())
            .build();

        mindmapRepository.save(mindmap);

        // 마인드맵 소유자 등록
        mindmapMemberRepository.save(
            MindmapMember.of(mindmap, user, MindmapRole.OWNER)
        );

        // 방문 기록 생성
        visitHistoryService.createVisitHistory(user, mindmap);

        return mindmapMapper.toResponseDto(mindmap);

    }

    /**
     * 특정 사용자의 "확인용 (n)" 다음 시퀀스 번호를 찾습니다.
     * @param user 대상 사용자
     * @return 다음 시퀀스 번호
     */
    private long findNextCheckSequence(User user) {
        // repo 조건이 제거된 리포지토리 메서드 호출
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
     * 마인드맵 상세 정보 조회
     * 저장소 업데이트는 Fast API Webhook 알림
     * 마인드맵 변경이나 리뷰 생성 시 SSE 적용을 통한 실시간 업데이트 (새로고침 x)
     */
    @Transactional
    public MindmapDetailResponseDto getMindmap(Long mapId, Long userId) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS); // 멤버 권한 확인
        }

        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        return mindmapMapper.toDetailResponseDto(mindmap);
    }

    /**
     * 마인드맵 새로고침
     */
    @Transactional
    public MindmapDetailResponseDto refreshMindmap(Long mapId, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        // 마인드맵 생성자만 새로고침 가능
        if (!mindmap.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 기존 정보로 FastAPI 재호출
        AnalysisResultDto dto = fastApiClient.analyze(
            mindmap.getRepo().getGithubRepoUrl(),
            mindmap.getPrompt(),
            mindmap.getType()
        );

        // 데이터 최신화
        mindmap.getRepo().updateWithAnalysis(dto);
        mindmap.updateMapData(dto.getMapData());

        MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap);

        // 업데이트된 마인드맵 정보를 모든 구독자에게 방송
        mindmapSseService.broadcastUpdate(mapId, responseDto);

        return responseDto;
    }

    /**
     * 마인드맵 삭제
     */
    @Transactional
    public void deleteMindmap(Long mapId, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        // 마인드맵 생성자만 삭제 가능
        if (!mindmap.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        mindmapRepository.delete(mindmap);
    }

}