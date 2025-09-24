package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.dto.NodeDto;
import com.teamEWSN.gitdeun.mindmap.dto.FileWithCodeDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.RelatedFileDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapGraphResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.NodeCodeResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.NodeSimpleDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmap.util.FileContentCache;
import com.teamEWSN.gitdeun.mindmap.util.MindmapGraphCache;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NodeService {
    private final MindmapAuthService mindmapAuthService;
    private final MindmapRepository mindmapRepository;
    private final MindmapGraphCache mindmapGraphCache;
    private final FileContentCache fileContentCache;

    /**
     * (테스트용) 마인드맵의 모든 노드 목록을 간략하게 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<NodeSimpleDto> getNodeList(Long mapId, String authorizationHeader) {
        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
                .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        String repoUrl = mindmap.getRepo().getGithubRepoUrl();
        LocalDateTime lastCommit = mindmap.getRepo().getLastCommit(); // lastCommit 정보 가져오기

        MindmapGraphResponseDto graphData = mindmapGraphCache.getGraphWithHybridCache(repoUrl, lastCommit, authorizationHeader);
        if (graphData == null || graphData.getNodes() == null) {
            return Collections.emptyList();
        }

        return graphData.getNodes().stream()
                .map(node -> new NodeSimpleDto(node.getKey(), node.getLabel()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NodeCodeResponseDto getNodeDetailsWithCode(
            Long mapId,
            String nodeKey,
            Long userId,
            String authorizationHeader
    ) {
        // 0) 권한 체크
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 1) 맵/레포 컨텍스트 로드
        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
                .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        String repoUrl = mindmap.getRepo().getGithubRepoUrl();
        LocalDateTime lastCommit = mindmap.getRepo().getLastCommit();

        // 2) 그래프 데이터 로드
        MindmapGraphResponseDto graphData =
                mindmapGraphCache.getGraphWithHybridCache(repoUrl, lastCommit, authorizationHeader);
        if (graphData == null || !Boolean.TRUE.equals(graphData.getSuccess())) {
            throw new GlobalException(ErrorCode.MINDMAP_NOT_FOUND);
        }

        // 3) 대상 노드 찾기
        NodeDto targetNode = graphData.getNodes().stream()
                .filter(n -> nodeKey.equals(n.getKey()))
                .findFirst()
                .orElseThrow(() -> new GlobalException(ErrorCode.NODE_NOT_FOUND));

        List<RelatedFileDto> related = targetNode.getRelatedFiles();
        if (related == null || related.isEmpty()) {
            return new NodeCodeResponseDto(
                    targetNode.getKey(),
                    targetNode.getLabel(),
                    Collections.emptyList()
            );
        }

        // 4) 각 파일에 대해 FastAPI(by-node)로 코드 조회
        //    prefer=auto => AI 코드가 있으면 AI, 없으면 원본으로 자동 폴백
        List<FileWithCodeDto> filesWithCode = related.parallelStream()
                .map(rf -> {
                    String code = null;
                    try {
                        code = fileContentCache.getFileContentByNodeWithCache(
                                repoUrl,
                                nodeKey,
                                rf.getFilePath(),   // 파일명만 와도 FastAPI가 경로 해석함
                                "auto",             // ai 우선, 없으면 original 폴백
                                lastCommit,
                                authorizationHeader
                        );
                    } catch (Exception ignore) {
                        // 개별 파일 실패는 무시하고 다음 파일 계속
                    }
                    return new FileWithCodeDto(rf, code == null ? "" : code);
                })
                .collect(Collectors.toList());

        // 5) 응답
        return new NodeCodeResponseDto(
                targetNode.getKey(),
                targetNode.getLabel(),
                filesWithCode
        );
    }
}