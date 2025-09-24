package com.teamEWSN.gitdeun.codereference.service;

import com.teamEWSN.gitdeun.codereference.dto.CodeReferenceRequestDtos.*;
import com.teamEWSN.gitdeun.codereference.dto.CodeReferenceResponseDtos.*;
import com.teamEWSN.gitdeun.codereference.entity.CodeReference;
import com.teamEWSN.gitdeun.codereference.mapper.CodeReferenceMapper;
import com.teamEWSN.gitdeun.codereference.repository.CodeReferenceRepository;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.NodeDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapGraphResponseDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmap.util.FileContentCache;
import com.teamEWSN.gitdeun.mindmap.util.MindmapGraphCache;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodeReferenceService {

    private final CodeReferenceRepository codeReferenceRepository;
    private final MindmapRepository mindmapRepository;
    private final MindmapAuthService mindmapAuthService;
    private final CodeReferenceMapper codeReferenceMapper;
    private final MindmapGraphCache mindmapGraphCache;
    private final FileContentCache fileContentCache;
    private final FastApiClient fastApiClient;

    @Transactional
    public ReferenceResponse createReference(Long mapId, String nodeKey, Long userId, CreateRequest request, String authorizationHeader) {
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findById(mapId)
                .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        validateCodeReferenceRequest(mindmap, nodeKey, request, authorizationHeader);

        CodeReference codeReference = CodeReference.builder()
                .mindmap(mindmap)
                .nodeKey(nodeKey)
                .filePath(request.getFilePath().trim())
                .startLine(request.getStartLine())
                .endLine(request.getEndLine())
                .build();

        CodeReference saved = codeReferenceRepository.save(codeReference);
        return codeReferenceMapper.toReferenceResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReferenceDetailResponse getReferenceDetail(Long mapId, Long refId, Long userId, String authorizationHeader) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        CodeReference ref = codeReferenceRepository.findByMindmapIdAndId(mapId, refId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CODE_REFERENCE_NOT_FOUND));

        Mindmap mindmap = ref.getMindmap();
        String fullContent = fastApiClient.getFileRaw(
                mindmap.getRepo().getGithubRepoUrl(),
                ref.getFilePath(),
                authorizationHeader
        );

        String snippet = extractLines(fullContent, ref.getStartLine(), ref.getEndLine());
        return codeReferenceMapper.toReferenceDetailResponse(ref, snippet);
    }

    @Transactional(readOnly = true)
    public List<ReferenceResponse> getReferencesForNode(Long mapId, String nodeKey, Long userId) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return codeReferenceRepository.findByMindmapIdAndNodeKey(mapId, nodeKey)
                .stream()
                .map(codeReferenceMapper::toReferenceResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReferenceResponse updateReference(Long mapId, Long refId, Long userId, CreateRequest request, String authorizationHeader) {
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        CodeReference ref = codeReferenceRepository.findByMindmapIdAndId(mapId, refId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CODE_REFERENCE_NOT_FOUND));

        validateCodeReferenceRequest(ref.getMindmap(), ref.getNodeKey(), request, authorizationHeader);

        ref.update(request.getFilePath().trim(), request.getStartLine(), request.getEndLine());

        return codeReferenceMapper.toReferenceResponse(ref);
    }

    @Transactional
    public void deleteReference(Long mapId, Long refId, Long userId) {
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }
        if (!codeReferenceRepository.existsByMindmapIdAndId(mapId, refId)) {
            throw new GlobalException(ErrorCode.CODE_REFERENCE_NOT_FOUND);
        }
        codeReferenceRepository.deleteById(refId);
    }

    /** ---------- helpers ---------- */

    private String extractLines(String fullContent, Integer startLine, Integer endLine) {
        if (fullContent == null) return "";
        if (startLine == null || endLine == null || startLine <= 0 || endLine < startLine) {
            return fullContent; // 라인 지정이 없으면 전체 반환
        }
        String[] lines = fullContent.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (int i = startLine - 1; i < endLine && i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }

    private void validateCodeReferenceRequest(Mindmap mindmap, String nodeKey, CreateRequest request, String authorizationHeader) {
        String repoUrl = mindmap.getRepo().getGithubRepoUrl();
        LocalDateTime lastCommit = mindmap.getRepo().getLastCommit();

        // 그래프 로드
        MindmapGraphResponseDto graphData =
                mindmapGraphCache.getGraphWithHybridCache(repoUrl, lastCommit, authorizationHeader);

        NodeDto targetNode = graphData.getNodes().stream()
                .filter(n -> nodeKey.equals(n.getKey()))
                .findFirst()
                .orElseThrow(() -> new GlobalException(ErrorCode.NODE_NOT_FOUND));

        String reqPath = request.getFilePath() == null ? "" : request.getFilePath().trim();

        // 노드에 연결된 파일 목록 안에 존재 여부 (정확히 일치 또는 말미 일치 허용)
        boolean fileExists = targetNode.getRelatedFiles().stream().anyMatch(rf -> {
            String fp = rf.getFilePath() == null ? "" : rf.getFilePath();
            return fp.equals(reqPath)
                    || fp.endsWith("/" + reqPath)
                    || reqPath.endsWith("/" + fp);
        });

        if (!fileExists) {
            throw new GlobalException(ErrorCode.FILE_NOT_FOUND_IN_NODE);
        }

        // 라인 검증: start/end 둘 다 있을 때만 검사
        Integer start = request.getStartLine();
        Integer end = request.getEndLine();
        if (start != null && end != null) {
            String fileContent = fileContentCache.getFileContentWithCache(
                    repoUrl, resolvePath(reqPath, targetNode), lastCommit, authorizationHeader
            );
            int totalLines = fileContent == null ? 0 : fileContent.split("\\r?\\n").length;
            if (start <= 0 || start > end || end > totalLines) {
                throw new GlobalException(ErrorCode.INVALID_LINE_RANGE);
            }
        }
    }

    // 노드의 related_files 중 요청 경로와 가장 잘 맞는 실제 경로를 반환
    private String resolvePath(String reqPath, NodeDto node) {
        return node.getRelatedFiles().stream()
                .map(rf -> rf.getFilePath())
                .filter(fp -> fp != null && (fp.equals(reqPath) || fp.endsWith("/" + reqPath) || reqPath.endsWith("/" + fp)))
                .findFirst()
                .orElse(reqPath);
    }
}
