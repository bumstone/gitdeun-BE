package com.teamEWSN.gitdeun.Recruitment.service;

import com.teamEWSN.gitdeun.Recruitment.dto.RecruitmentCreateRequestDto;
import com.teamEWSN.gitdeun.Recruitment.dto.RecruitmentDetailResponseDto;
import com.teamEWSN.gitdeun.Recruitment.dto.RecruitmentListResponseDto;
import com.teamEWSN.gitdeun.Recruitment.dto.RecruitmentUpdateRequestDto;
import com.teamEWSN.gitdeun.Recruitment.entity.*;
import com.teamEWSN.gitdeun.Recruitment.mapper.RecruitmentMapper;
import com.teamEWSN.gitdeun.Recruitment.repository.RecruitmentImageRepository;
import com.teamEWSN.gitdeun.Recruitment.repository.RecruitmentRepository;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.s3.service.S3BucketService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruitmentService {
    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentImageRepository recruitmentImageRepository;
    private final UserRepository userRepository;
    private final RecruitmentMapper recruitmentMapper;
    private final S3BucketService s3BucketService;


    /**
     * 새로운 모집 공고를 생성합니다.
     * 모집 기간에 따라 초기 상태(모집 예정, 모집 중)가 자동으로 설정됩니다.
     * @param userId 모집 공고를 생성하는 사용자 ID
     * @param requestDto 생성할 공고의 정보가 담긴 DTO
     * @return 생성된 공고의 상세 정보 DTO
     */
    @Transactional
    public RecruitmentDetailResponseDto createRecruitment(Long userId, RecruitmentCreateRequestDto requestDto) {
        User recruiter = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        validateRecruitmentDates(requestDto.getStartAt(), requestDto.getEndAt());

        // DTO를 엔티티로 변환
        Recruitment recruitment = recruitmentMapper.toEntity(requestDto);
        recruitment.setRecruiter(recruiter);

        // 상태 설정
        RecruitmentStatus initialStatus = requestDto.getStartAt().isAfter(LocalDateTime.now()) ?
            RecruitmentStatus.FORTHCOMING : RecruitmentStatus.RECRUITING;
        recruitment.setStatus(initialStatus);

        // Recruitment 엔티티를 먼저 저장 (ID 생성을 위해)
        Recruitment savedRecruitment = recruitmentRepository.save(recruitment);

        // requiredSkills 처리
        processRequiredSkills(savedRecruitment, requestDto.getRequiredSkills());

        // 이미지 파일 처리
        if (!CollectionUtils.isEmpty(requestDto.getImages())) {
            // S3에 파일 업로드 후 URL 리스트를 받아옴
            String s3Path = "recruitments/" + savedRecruitment.getId();
            List<String> uploadedUrls = s3BucketService.upload(requestDto.getImages(), s3Path);

            // URL을 RecruitmentImage 엔티티로 변환
            List<RecruitmentImage> images = uploadedUrls.stream()
                .map(url -> RecruitmentImage.builder()
                    .imageUrl(url)
                    .recruitment(savedRecruitment)
                    .build())
                .collect(Collectors.toList());

            // 이미지 정보 저장
            recruitmentImageRepository.saveAll(images);
            savedRecruitment.setRecruitmentImages(images);
        }

        return recruitmentMapper.toDetailResponseDto(savedRecruitment);
    }

    /**
     * 현재 로그인한 사용자가 작성한 모든 모집 공고 목록을 조회합니다.
     * @param userId 현재 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징 처리된 내 모집 공고 목록
     */
    @Transactional(readOnly = true)
    public Page<RecruitmentListResponseDto> getMyRecruitments(Long userId, Pageable pageable) {
        return recruitmentRepository.findByRecruiterId(userId, pageable)
            .map(recruitmentMapper::toListResponseDto);
    }

    /**
     * 특정 모집 공고의 상세 정보를 조회합니다.
     * 이 메서드가 호출될 때마다 해당 공고의 조회수가 1 증가합니다.
     * @param recruitmentId 조회할 공고의 ID
     * @return 공고의 상세 정보 DTO
     */
    @Transactional
    public RecruitmentDetailResponseDto getRecruitment(Long recruitmentId) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.RECRUITMENT_NOT_FOUND));

        recruitment.increaseView();
        return recruitmentMapper.toDetailResponseDto(recruitment);
    }

    /**
     * 상태(status)와 모집 분야(field)를 기준으로 모집 공고를 필터링하여 검색합니다.
     * @param status 검색할 모집 상태 (선택 사항)
     * @param fields 검색할 모집 분야 목록 (선택 사항)
     * @param pageable 페이징 정보
     * @return 페이징 처리된 검색 결과 목록
     */
    @Transactional(readOnly = true)
    public Page<RecruitmentListResponseDto> searchRecruitments(RecruitmentStatus status, List<RecruitmentField> fields, Pageable pageable) {
        return recruitmentRepository.searchRecruitments(status, fields, pageable).map(this::addThumbnailUrl);
    }

    /**
     * 특정 모집 공고를 수정합니다.
     * 공고 작성자만 수정할 수 있습니다.
     * @param recruitmentId 수정할 공고의 ID
     * @param userId 요청한 사용자의 ID
     * @param requestDto 수정할 공고의 정보가 담긴 DTO
     * @return 수정된 공고의 상세 정보 DTO
     */
    @Transactional
    public RecruitmentDetailResponseDto updateRecruitment(Long recruitmentId, Long userId, RecruitmentUpdateRequestDto requestDto) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.RECRUITMENT_NOT_FOUND));

        if (!recruitment.getRecruiter().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        validateRecruitmentDates(recruitment.getStartAt(), requestDto.getEndAt());

        recruitmentMapper.updateRecruitmentFromDto(requestDto, recruitment);

        // 기존 requiredSkills 제거
        recruitment.getRequiredSkills().clear();

        // 새로운 requiredSkills 설정
        processRequiredSkills(recruitment, requestDto.getRequiredSkills());

        // 이미지 업데이트 로직
        updateImages(recruitment, requestDto.getKeepImageIds(), requestDto.getNewImages());

        return recruitmentMapper.toDetailResponseDto(recruitment);
    }

    /**
     * 게시글의 이미지를 업데이트합니다.
     * @param recruitment 이미지를 업데이트할 Recruitment 엔티티
     * @param keepImageIds 유지할 기존 이미지의 ID 리스트
     * @param newImages 새로 추가할 이미지 파일 리스트
     */
    private void updateImages(Recruitment recruitment, List<Long> keepImageIds, List<MultipartFile> newImages) {
        // 1. 기존 이미지 중 유지하지 않는 것만 soft-delete
        List<RecruitmentImage> existingImages = recruitmentImageRepository.findByRecruitmentAndDeletedAtIsNull(recruitment);
        List<Long> finalKeepIds = keepImageIds == null ? new ArrayList<>() : keepImageIds;

        List<RecruitmentImage> imagesToDelete = existingImages.stream()
            .filter(img -> !finalKeepIds.contains(img.getId()))
            .collect(Collectors.toList());

        if (!imagesToDelete.isEmpty()) {
            imagesToDelete.forEach(RecruitmentImage::softDelete);
            recruitmentImageRepository.saveAll(imagesToDelete);
        }

        // 2. 새 이미지 S3에 업로드 및 DB에 저장
        if (!CollectionUtils.isEmpty(newImages)) {
            String s3Path = "recruitments/" + recruitment.getId();
            List<String> uploadedUrls = s3BucketService.upload(newImages, s3Path);

            List<RecruitmentImage> imagesToAdd = uploadedUrls.stream()
                .map(url -> RecruitmentImage.builder().imageUrl(url).recruitment(recruitment).build())
                .collect(Collectors.toList());
            recruitmentImageRepository.saveAll(imagesToAdd);
        }
    }

    /**
     * 특정 모집 공고를 삭제합니다.
     * 공고 작성자만 삭제할 수 있습니다.
     * @param recruitmentId 삭제할 공고의 ID
     * @param userId 요청한 사용자의 ID
     */
    @Transactional
    public void deleteRecruitment(Long recruitmentId, Long userId) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.RECRUITMENT_NOT_FOUND));

        if (!recruitment.getRecruiter().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }
        recruitmentRepository.delete(recruitment);
    }

    /**
     * 사용자의 기술 스택을 기반으로 맞춤 모집 공고를 추천합니다.
     * 사용자가 보유한 기술과 공고의 요구 기술이 많이 일치할수록 상단에 노출됩니다.
     * @param userId 추천을 받을 사용자의 ID
     * @param pageable 페이징 정보
     * @return 페이징 처리된 추천 공고 목록
     */
    @Transactional(readOnly = true)
    public Page<RecruitmentListResponseDto> getRecommendedRecruitments(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Set<DeveloperSkill> userSkills = user.getSkills().stream()
            .map(userSkill -> DeveloperSkill.valueOf(userSkill.getSkill().toUpperCase()))
            .collect(Collectors.toSet());

        // Repository에서 기본 데이터만 조회
        List<Recruitment> recruitments = recruitmentRepository.findAllByStatusWithRequiredSkills(RecruitmentStatus.RECRUITING);

        List<RecruitmentListResponseDto> matchedRecruitments = recruitments.stream()
            .map(recruitment -> calculateRecommendationScore(recruitment, userSkills))
            .filter(dto -> dto.getMatchScore() > 0.0) // 하나라도 일치하는 기술이 있는 공고만
            .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore())) // 점수 높은 순
            .collect(Collectors.toList());

        // 페이징 적용
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), scoredRecruitments.size());
        List<RecruitmentListResponseDto> pagedContent = scoredRecruitments.subList(start, end);

        return new PageImpl<>(pagedContent, pageable, scoredRecruitments.size());
    }

    /** Helper Method */
    private RecruitmentListResponseDto addThumbnailUrl(RecruitmentListResponseDto dto) {
        if (dto.getThumbnailUrl() == null) {
            // 첫 번째 이미지 조회
            List<RecruitmentImage> images = recruitmentImageRepository.findByRecruitmentIdAndDeletedAtIsNull(dto.getId());
            String thumbnailUrl = images.stream()
                .findFirst()
                .map(RecruitmentImage::getImageUrl)
                .orElse(null);

            return RecruitmentListResponseDto.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .thumbnailUrl(thumbnailUrl)
                .status(dto.getStatus())
                .languageTags(dto.getLanguageTags())
                .fieldTags(dto.getFieldTags())
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .recruitQuota(dto.getRecruitQuota())
                .build();
        }
        return dto;
    }

    private void processRequiredSkills(Recruitment recruitment, Set<DeveloperSkill> requiredSkillsDto) {
        if (requiredSkillsDto == null || requiredSkillsDto.isEmpty()) {
            return;
        }

        Set<RecruitmentRequiredSkill> requiredSkills = requiredSkillsDto.stream()
            .map(skill -> RecruitmentRequiredSkill.builder()
                .recruitment(recruitment)
                .skill(skill)
                .category(SkillCategory.LANGUAGE) // 기본값으로 LANGUAGE 설정
                .weight(1.0) // 기본 가중치
                .build())
            .collect(Collectors.toSet());

        recruitment.setRequiredSkills(requiredSkills);
    }

    private void validateRecruitmentDates(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt.isAfter(endAt)) {
            throw new GlobalException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (endAt.isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.END_DATE_IN_PAST);
        }
    }


    // 추천 공고 가중치 계산
    private RecruitmentListResponseDto calculateRecommendationScore(Recruitment recruitment, Set<DeveloperSkill> userSkills) {
        double score = 0.0;

        // 1차 매칭: requiredSkills 기반 (가중치 적용)
        Set<RecruitmentRequiredSkill> requiredSkills = recruitment.getRequiredSkills();
        if (!requiredSkills.isEmpty()) {
            score = calculateWeightedScore(requiredSkills, userSkills);
        } else {
            // 2차 매칭: languageTags 기반 (fallback)
            score = calculateSimpleScore(recruitment.getLanguageTags(), userSkills);
        }

        // 최근 공고 보너스
        if (recruitment.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7))) {
            score = Math.min(score + 0.05, 1.0);
        }

        return recruitmentMapper.toListResponseDto(recruitment);
    }

    private double calculateWeightedScore(Set<RecruitmentRequiredSkill> requiredSkills, Set<DeveloperSkill> userSkills) {
        double totalWeight = 0.0;
        double matchedWeight = 0.0;

        for (RecruitmentRequiredSkill required : requiredSkills) {
            if (required.getCategory() == SkillCategory.LANGUAGE) {
                double weight = required.getWeight(); // 기본값(1.0)이든 커스텀이든 상관없이 사용
                totalWeight += weight;

                if (userSkills.contains(required.getSkill())) {
                    matchedWeight += weight;
                }
            }
        }

        return totalWeight > 0 ? matchedWeight / totalWeight : 0.0;
    }

    private double calculateSimpleScore(Set<DeveloperSkill> languageTags, Set<DeveloperSkill> userSkills) {
        if (languageTags.isEmpty()) return 0.0;

        long matchCount = languageTags.stream()
            .mapToLong(tag -> userSkills.contains(tag) ? 1 : 0)
            .sum();

        return (double) matchCount / languageTags.size();
    }

}

