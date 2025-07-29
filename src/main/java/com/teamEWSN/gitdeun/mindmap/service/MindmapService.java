package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapCreateRequest;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapResponseDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.entity.MindmapType;
import com.teamEWSN.gitdeun.mindmap.mapper.MindmapMapper;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import com.teamEWSN.gitdeun.repo.repository.RepoRepository;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
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

    private final MindmapMapper mindmapMapper;
    private final MindmapRepository mindmapRepository;
    private final RepoRepository repoRepository;
    private final UserRepository userRepository;
    private final FastApiClient fastApiClient;

    @Transactional
    public MindmapResponseDto createMindmap(MindmapCreateRequest req, Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Repo repo = repoRepository.findByGithubRepoUrl(req.getRepoUrl())
            .orElseGet(() -> repoRepository.save(Repo.builder().githubRepoUrl(req.getRepoUrl()).build()));

        AnalysisResultDto dto = fastApiClient.analyze(req.getRepoUrl(), req.getPrompt(), req.getType());

        repo.updateWithAnalysis(dto);
        repoRepository.save(repo);             // dirty-checking

        String field;

        if (req.getType() == MindmapType.DEV) {
            field = "개발용";
        } else {
            if (req.getTitle() != null && !req.getTitle().isEmpty()) {
                field = req.getTitle();
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


}