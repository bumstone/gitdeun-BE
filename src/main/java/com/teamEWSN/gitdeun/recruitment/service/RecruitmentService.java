package com.teamEWSN.gitdeun.recruitment.service;

import com.teamEWSN.gitdeun.recruitment.dto.*;
import com.teamEWSN.gitdeun.recruitment.entity.*;
import com.teamEWSN.gitdeun.recruitment.mapper.RecruitmentMapper;
import com.teamEWSN.gitdeun.recruitment.repository.RecruitmentImageRepository;
import com.teamEWSN.gitdeun.recruitment.repository.RecruitmentRepository;
import com.teamEWSN.gitdeun.recruitment.service.util.RecommendationScoreCalculator;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.s3.service.S3BucketService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
     * @param userId 모집 공고를 생성하는 사용자 ID
     * @param requestDto 생성할 공고의 정보가 담긴 DTO
     * @return 생성된 공고의 상세 정보 DTO
     */
    @Transactional
    public RecruitmentDetailResponseDto createRecruitment(Long userId, RecruitmentCreateRequestDto requestDto, List<MultipartFile> images) {
        User recruiter = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 마감일을 해당 날짜의 23:59:59로 설정
        LocalDateTime adjustedEndAt = requestDto.getEndAt().with(LocalTime.of(23, 59, 59));

        validateRecruitmentDates(requestDto.getStartAt(), adjustedEndAt);

        Recruitment recruitment = recruitmentMapper.toEntity(requestDto);
        recruitment.setRecruiter(recruiter);
        recruitment.setEndAt(adjustedEndAt);

        RecruitmentStatus initialStatus = requestDto.getStartAt().isAfter(LocalDateTime.now()) ?
            RecruitmentStatus.FORTHCOMING : RecruitmentStatus.RECRUITING;
        recruitment.setStatus(initialStatus);

        Recruitment savedRecruitment = recruitmentRepository.save(recruitment);

        if (!CollectionUtils.isEmpty(images)) {
            List<RecruitmentImage> savedImages = uploadAndSaveImages(savedRecruitment, images);
            savedRecruitment.setRecruitmentImages(savedImages);
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
        Page<Recruitment> recruitmentPage = recruitmentRepository.findByRecruiterId(userId, pageable);

        List<RecruitmentListResponseDto> content = recruitmentPage.getContent().stream()
            .map(recruitment -> {
                RecruitmentListResponseDto dto = recruitmentMapper.toListResponseDto(recruitment);
                // 썸네일 URL 설정
                return addThumbnailUrl(dto, recruitment.getRecruitmentImages());
            })
            .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, recruitmentPage.getTotalElements());
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
     * @param keyword 제목 검색 키워드 (선택 사항) - 부분 문자열 매칭
     * @param status  검색할 모집 상태 (선택 사항)
     * @param fields  검색할 모집 분야 목록 (선택 사항)
     * @param pageable 페이징 정보
     * @return 페이징 처리된 검색 결과 목록
     */
    @Transactional(readOnly = true)
    public Page<RecruitmentListResponseDto> searchRecruitments(String keyword, RecruitmentStatus status, List<RecruitmentField> fields, List<DeveloperSkill> languages, Pageable pageable) {
        return recruitmentRepository.searchRecruitments(keyword, status, fields, languages, pageable)
            .map(recruitmentMapper::toListResponseDto);
    }

    /**
     * 특정 모집 공고를 수정합니다.
     * 공고 작성자만 수정할 수 있습니다.
     * @param recruitmentId 수정할 공고의 ID
     * @param userId 요청한 사용자의 ID
     * @param requestDto 수정할 공고의 정보가 담긴 DTO
     * @param newImages 새로 추가할 이미지 파일 목록
     * @return 수정된 공고의 상세 정보 DTO
     */
    @Transactional
    public RecruitmentDetailResponseDto updateRecruitment(Long recruitmentId, Long userId, RecruitmentUpdateRequestDto requestDto, List<MultipartFile> newImages) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.RECRUITMENT_NOT_FOUND));

        if (!recruitment.getRecruiter().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // DTO에 endAt이 제공된 경우에만 날짜 유효성 검사
        if (requestDto.getEndAt() != null) {
            validateRecruitmentDates(recruitment.getStartAt(), requestDto.getEndAt());
        }

        recruitmentMapper.updateRecruitmentFromDto(requestDto, recruitment);

        // 이미지 업데이트 - 삭제 후 새 이미지 추가
        deleteUnusedImages(recruitment, requestDto.getKeepImageIds());

        if (!CollectionUtils.isEmpty(newImages)) {
            uploadAndSaveImages(recruitment, newImages);
        }

        return recruitmentMapper.toDetailResponseDto(recruitment);
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

        // 사용자 기술 스택 조회
        Set<DeveloperSkill> userSkills = user.getSkills().stream()
            .map(userSkill -> DeveloperSkill.valueOf(userSkill.getSkill().toUpperCase()))
            .collect(Collectors.toSet());

        // RECRUITING과 FORTHCOMING 상태인 공고만 조회
        List<RecruitmentStatus> targetStatuses = List.of(
            RecruitmentStatus.RECRUITING,
            RecruitmentStatus.FORTHCOMING
        );

        List<Recruitment> recruitments = recruitmentRepository.findAllByStatusIn(targetStatuses);

        // 매칭 점수 계산 및 정렬
        List<RecruitmentListResponseDto> matchedRecruitments = recruitments.stream()
            .map(recruitment -> {
                // 기본 DTO 생성
                RecruitmentListResponseDto dto = recruitmentMapper.toListResponseDto(recruitment);

                // 썸네일 URL 설정
                dto = addThumbnailUrl(dto, recruitment.getRecruitmentImages());

                // 매칭 점수 계산 및 설정
                double matchScore = RecommendationScoreCalculator.calculate(recruitment, userSkills);
                dto = withMatchScore(dto, matchScore);

                return dto;
            })
            .filter(dto -> dto.getMatchScore() > 0.0) // 매칭 점수가 0인 경우 제외
            .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore())) // 점수 높은 순 정렬
            .collect(Collectors.toList());

        // 페이징 적용
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), matchedRecruitments.size());
        List<RecruitmentListResponseDto> pagedContent = start < matchedRecruitments.size() ?
            matchedRecruitments.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(pagedContent, pageable, matchedRecruitments.size());
    }

    // =============== Private Helper Methods ===============

    /**
     * DTO에 썸네일 URL을 추가합니다.
     */
    private RecruitmentListResponseDto addThumbnailUrl(RecruitmentListResponseDto dto, List<RecruitmentImage> images) {
        if (dto.getThumbnailUrl() != null) {
            return dto;
        }

        String thumbnailUrl = images.stream()
            .filter(image -> image.getDeletedAt() == null)
            .findFirst()
            .map(RecruitmentImage::getImageUrl)
            .orElse(null);

        return withThumbnailUrl(dto, thumbnailUrl);
    }

    /**
     * DTO에 매칭 점수를 추가합니다.
     */
    private RecruitmentListResponseDto withMatchScore(RecruitmentListResponseDto dto, Double matchScore) {
        return RecruitmentListResponseDto.builder()
            .id(dto.getId())
            .title(dto.getTitle())
            .thumbnailUrl(dto.getThumbnailUrl())
            .status(dto.getStatus())
            .languageTags(dto.getLanguageTags())
            .fieldTags(dto.getFieldTags())
            .startAt(dto.getStartAt())
            .endAt(dto.getEndAt())
            .recruitQuota(dto.getRecruitQuota())
            .viewCount(dto.getViewCount())
            .matchScore(matchScore)
            .build();
    }

    /**
     * DTO에 썸네일 URL을 추가합니다.
     */
    private RecruitmentListResponseDto withThumbnailUrl(RecruitmentListResponseDto dto, String thumbnailUrl) {
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
            .viewCount(dto.getViewCount())
            .matchScore(dto.getMatchScore())
            .build();
    }

    /**
     * 이미지 파일들을 S3에 업로드하고 DB에 저장합니다.
     * @param recruitment 이미지를 연결할 공고 엔티티
     * @param imageFiles 업로드할 이미지 파일들
     * @return 저장된 이미지 엔티티 목록
     */
    private List<RecruitmentImage> uploadAndSaveImages(Recruitment recruitment, List<MultipartFile> imageFiles) {
        if (CollectionUtils.isEmpty(imageFiles)) {
            return Collections.emptyList();
        }

        // S3에 이미지 업로드
        String s3Path = "recruitments/" + recruitment.getId();
        List<String> uploadedUrls = s3BucketService.upload(imageFiles, s3Path);

        // 이미지 엔티티 생성
        List<RecruitmentImage> images = uploadedUrls.stream()
            .map(url -> RecruitmentImage.builder()
                .imageUrl(url)
                .recruitment(recruitment)
                .build())
            .collect(Collectors.toList());

        // DB에 저장
        return recruitmentImageRepository.saveAll(images);
    }

    /**
     * 기존 이미지 중 유지할 ID 목록에 없는 이미지를 소프트 삭제합니다.
     *
     * @param recruitment  공고 엔티티
     * @param keepImageIds 유지할 이미지 ID 목록
     */
    private void deleteUnusedImages(Recruitment recruitment, List<Long> keepImageIds) {
        List<RecruitmentImage> existingImages = recruitmentImageRepository.findByRecruitmentAndDeletedAtIsNull(recruitment);
        List<Long> finalKeepIds = keepImageIds == null ? Collections.emptyList() : keepImageIds;

        List<RecruitmentImage> imagesToDelete = existingImages.stream()
            .filter(img -> !finalKeepIds.contains(img.getId()))
            .collect(Collectors.toList());

        if (!imagesToDelete.isEmpty()) {
            imagesToDelete.forEach(RecruitmentImage::softDelete);
            recruitmentImageRepository.saveAll(imagesToDelete);
        }

    }

    /**
     * 모집 공고의 날짜 유효성을 검사합니다.
     */
    private void validateRecruitmentDates(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt.isAfter(endAt)) {
            throw new GlobalException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (endAt.isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.END_DATE_IN_PAST);
        }
    }
}
