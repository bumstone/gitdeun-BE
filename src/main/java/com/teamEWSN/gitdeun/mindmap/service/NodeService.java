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

        // 4) 각 파일에 대해 FastAPI(by-node)로 코드 조회 (prefer=auto)
        List<FileWithCodeDto> filesWithCode = related.parallelStream()
                .map(rf -> {
                    String code = null;
                    try {
                        // ★ 인자 순서 주의: (repoUrl, nodeKey, filePath, prefer, lastCommit, authorizationHeader)
                        code = fileContentCache.getFileContentByNodeWithCache(
                                repoUrl,               // 1
                                nodeKey,               // 2
                                rf.getFilePath(),      // 3
                                "auto",                // 4 (ai 우선, 없으면 original)
                                lastCommit,            // 5
                                authorizationHeader    // 6 (없으면 null 들어가도 됨)
                        );
                    } catch (Exception ignore) {
                    }

                    // ★ AI 실패 시 원본 폴백
                    if (code == null || code.isBlank()) {
                        try {
                            code = fileContentCache.getFileContentWithCache(
                                    repoUrl,
                                    rf.getFilePath(),
                                    lastCommit,
                                    authorizationHeader
                            );
                        } catch (Exception ignore) {
                        }
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