package com.teamEWSN.gitdeun.codereference.service;


import com.teamEWSN.gitdeun.codereference.dto.CodeReferenceRequestDtos.*;
import com.teamEWSN.gitdeun.codereference.dto.CodeReferenceResponseDtos.*;
import com.teamEWSN.gitdeun.codereference.entity.CodeReference;
import com.teamEWSN.gitdeun.codereference.mapper.CodeReferenceMapper;
import com.teamEWSN.gitdeun.codereference.repository.CodeReferenceRepository;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodeReferenceService {

    private final CodeReferenceRepository codeReferenceRepository;
    private final MindmapRepository mindmapRepository;
    private final MindmapAuthService mindmapAuthService;
    private final CodeReferenceMapper codeReferenceMapper;

    @Transactional
    public ReferenceResponse createReference(Long mapId, String nodeId, Long userId, CreateRequest request) {
        // 1. 권한 확인
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 2. 마인드맵 존재 여부 확인
        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        // 3. CodeReference 엔티티 생성 및 저장
        CodeReference codeReference = CodeReference.builder()
            .mindmap(mindmap)
            .nodeId(nodeId)
            .filePath(request.getFilePath())
            .startLine(request.getStartLine())
            .endLine(request.getEndLine())
            .build();

        CodeReference savedReference = codeReferenceRepository.save(codeReference);

        // 4. DTO로 변환하여 반환
        return codeReferenceMapper.toReferenceResponse(savedReference);
    }

    @Transactional(readOnly = true)
    public ReferenceResponse getReference(Long mapId, Long refId, Long userId) {
        // 1. 권한 확인
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 2. 해당 마인드맵에 속한 코드 참조인지 확인
        CodeReference codeReference = codeReferenceRepository.findByMindmapIdAndId(mapId, refId)
            .orElseThrow(() -> new GlobalException(ErrorCode.CODE_REFERENCE_NOT_FOUND));

        // 3. DTO로 변환하여 반환
        return codeReferenceMapper.toReferenceResponse(codeReference);
    }

    @Transactional(readOnly = true)
    public List<ReferenceResponse> getReferencesForNode(Long mapId, String nodeId, Long userId) {
        // 1. 권한 확인 (읽기)
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 2. 특정 노드에 속한 모든 코드 참조 조회
        List<CodeReference> references = codeReferenceRepository.findByMindmapIdAndNodeId(mapId, nodeId);

        // 3. DTO 리스트로 변환하여 반환
        return references.stream()
            .map(codeReferenceMapper::toReferenceResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ReferenceResponse updateReference(Long mapId, Long refId, Long userId, CreateRequest request) {
        // 1. 권한 확인 (쓰기)
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 2. 해당 마인드맵에 속한 코드 참조인지 확인 후 조회
        CodeReference codeReference = codeReferenceRepository.findByMindmapIdAndId(mapId, refId)
            .orElseThrow(() -> new GlobalException(ErrorCode.CODE_REFERENCE_NOT_FOUND));

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
}
