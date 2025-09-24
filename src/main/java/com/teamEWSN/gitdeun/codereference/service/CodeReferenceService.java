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
    public ReferenceResponse createReference(Long mapId, String nodeKey, Long userId, CreateRequest request, String authorizationHeader) throws GlobalException {
        // 1. 권한 확인
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 2. 마인드맵 존재 여부 확인
        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        validateCodeReferenceRequest(mindmap, nodeKey, request, authorizationHeader);

        // 3. CodeReference 엔티티 생성 및 저장
        CodeReference codeReference = CodeReference.builder()
            .mindmap(mindmap)
            .nodeKey(nodeKey)
            .filePath(request.getFilePath())
            .startLine(request.getStartLine())
            .endLine(request.getEndLine())
            .build();

        CodeReference savedReference = codeReferenceRepository.save(codeReference);

        return codeReferenceMapper.toReferenceResponse(savedReference);
    }

    @Transactional(readOnly = true)
    public ReferenceDetailResponse getReferenceDetail(Long mapId, Long refId, Long userId, String authorizationHeader) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        CodeReference codeReference = codeReferenceRepository.findByMindmapIdAndId(mapId, refId)
            .orElseThrow(() -> new GlobalException(ErrorCode.CODE_REFERENCE_NOT_FOUND));

        Mindmap mindmap = codeReference.getMindmap();

        // FastAPI를 통해 전체 파일 내용 가져오기
        String fullContent = fastApiClient.getFileRaw(mindmap.getRepo().getGithubRepoUrl(), codeReference.getFilePath(), authorizationHeader);

        // 특정 라인만 추출 (snippet)
        String snippet = extractLines(fullContent, codeReference.getStartLine(), codeReference.getEndLine());

        return codeReferenceMapper.toReferenceDetailResponse(codeReference, snippet);
    }

    @Transactional(readOnly = true)
    public List<ReferenceResponse> getReferencesForNode(Long mapId, String nodeKey, Long userId) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 2. 특정 노드에 속한 모든 코드 참조 조회
        List<CodeReference> references = codeReferenceRepository.findByMindmapIdAndNodeKey(mapId, nodeKey);

        // 3. DTO 리스트로 변환하여 반환
        return references.stream()
            .map(codeReferenceMapper::toReferenceResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ReferenceResponse updateReference(Long mapId, Long refId, Long userId, CreateRequest request, String authorizationHeader) {
        // 1. 권한 확인
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 2. 해당 마인드맵에 속한 코드 참조인지 확인 후 조회
        CodeReference codeReference = codeReferenceRepository.findByMindmapIdAndId(mapId, refId)
            .orElseThrow(() -> new GlobalException(ErrorCode.CODE_REFERENCE_NOT_FOUND));

        validateCodeReferenceRequest(codeReference.getMindmap(), codeReference.getNodeKey(), request, authorizationHeader);

        // 3. 엔티티 정보 업데이트
        codeReference.update(request.getFilePath(), request.getStartLine(), request.getEndLine());

        // 4. DTO로 변환하여 반환 (JPA의 Dirty Checking에 의해 자동 저장됨)
        return codeReferenceMapper.toReferenceResponse(codeReference);
    }

    @Transactional
    public void deleteReference(Long mapId, Long refId, Long userId) {
        // 1. 권한 확인
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 2. 해당 마인드맵에 코드 참조가 존재하는지 확인
        if (!codeReferenceRepository.existsByMindmapIdAndId(mapId, refId)) {
            throw new GlobalException(ErrorCode.CODE_REFERENCE_NOT_FOUND);
        }

        // 3. 코드 참조 삭제
        codeReferenceRepository.deleteById(refId);
    }

    private String extractLines(String fullContent, Integer startLine, Integer endLine) {
        if (fullContent == null) return "";
        if (startLine == null || endLine == null || startLine <= 0 || endLine < startLine) return fullContent;

        String[] lines = fullContent.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (int i = startLine - 1; i < endLine && i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }

    // 코드 참조 검증
    private void validateCodeReferenceRequest(Mindmap mindmap, String nodeKey, CreateRequest request, String authorizationHeader) {
        String repoUrl = mindmap.getRepo().getGithubRepoUrl();
        LocalDateTime lastCommit = mindmap.getRepo().getLastCommit();

        // 그래프 데이터에서 노드 정보 가져오기
        MindmapGraphResponseDto graphData = mindmapGraphCache.getGraphWithHybridCache(repoUrl, lastCommit, authorizationHeader);
        NodeDto targetNode = graphData.getNodes().stream()
            .filter(node -> nodeKey.equals(node.getKey()))
            .findFirst()
            .orElseThrow(() -> new GlobalException(ErrorCode.NODE_NOT_FOUND));

        // 요청된 filePath가 노드에 실제 포함된 파일인지 확인
        boolean fileExists = targetNode.getRelatedFiles().stream()
            .anyMatch(file -> request.getFilePath().equals(file.getFilePath()));
        if (!fileExists) {
            throw new GlobalException(ErrorCode.FILE_NOT_FOUND_IN_NODE);
        }

        // 파일의 전체 내용을 가져와 총 라인 수 계산
        String fileContent = fileContentCache.getFileContentWithCache(repoUrl, request.getFilePath(), lastCommit, authorizationHeader);
        int totalLines = fileContent.split("\\r?\\n").length;

        // 요청된 라인 범위(startLine, endLine)가 유효한지 확인
        Integer start = request.getStartLine();
        Integer end = request.getEndLine();
        if (start <= 0 || start > end || end > totalLines) {
            throw new GlobalException(ErrorCode.INVALID_LINE_RANGE);
        }
    }

}
