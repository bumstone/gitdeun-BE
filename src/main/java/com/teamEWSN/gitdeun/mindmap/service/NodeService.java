package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.MindmapGraphDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.NodeDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.RelatedFileDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapGraphResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.NodeCodeResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.NodeSimpleDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmap.util.MindmapGraphCache;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final FastApiClient fastApiClient;

    /**
     * (테스트용) 마인드맵의 모든 노드 목록을 간략하게 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<NodeSimpleDto> getNodeList(Long mapId, String authorizationHeader) {
        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        String repoUrl = mindmap.getRepo().getGithubRepoUrl();

        MindmapGraphResponseDto graphData = mindmapGraphCache.getGraphWithHybridCache(repoUrl, authorizationHeader);
        if (graphData == null || graphData.getNodes() == null) {
            return Collections.emptyList();
        }

        return graphData.getNodes().stream()
            .map(node -> new NodeSimpleDto(node.getKey(), node.getLabel()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NodeCodeResponseDto getNodeDetailsWithCode(Long mapId, String nodeKey, Long userId, String authorizationHeader) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        String repoUrl = mindmap.getRepo().getGithubRepoUrl();

        // 1. 캐시/API를 통해 그래프 데이터를 가져옵니다.
        MindmapGraphResponseDto graphData = mindmapGraphCache.getGraphWithHybridCache(repoUrl, authorizationHeader);
        if (graphData == null || !graphData.getSuccess()) {
            throw new GlobalException(ErrorCode.MINDMAP_NOT_FOUND);
        }

        // 2. 그래프에서 해당 nodeKey를 가진 노드를 찾습니다.
        NodeDto targetNode = graphData.getNodes().stream()
            .filter(node -> nodeKey.equals(node.getKey()))
            .findFirst()
            .orElseThrow(() -> new GlobalException(ErrorCode.NODE_NOT_FOUND));

        List<RelatedFileDto> filePaths = targetNode.getRelatedFiles();

        // 3. 파일 경로 목록을 사용하여 각 파일의 전체 코드를 가져옵니다.
        Map<RelatedFileDto, String> codeContents = filePaths.parallelStream()
            .collect(Collectors.toConcurrentMap(
                filePath -> filePath,
                filePath -> fastApiClient.getFileRaw(repoUrl, filePath.getFilePath(), authorizationHeader)
            ));

        return new NodeCodeResponseDto(
            targetNode.getKey(),
            targetNode.getLabel(),
            filePaths,
            codeContents
        );
    }
}

