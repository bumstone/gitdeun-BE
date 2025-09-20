package com.teamEWSN.gitdeun.Application.service;

import com.teamEWSN.gitdeun.Application.dto.*;
import com.teamEWSN.gitdeun.Application.entity.Application;
import com.teamEWSN.gitdeun.Application.entity.ApplicationStatus;
import com.teamEWSN.gitdeun.Application.mapper.ApplicationMapper;
import com.teamEWSN.gitdeun.Application.repository.ApplicationRepository;
import com.teamEWSN.gitdeun.recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.recruitment.entity.RecruitmentStatus;
import com.teamEWSN.gitdeun.recruitment.repository.RecruitmentRepository;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.notification.dto.NotificationCreateDto;
import com.teamEWSN.gitdeun.notification.entity.NotificationType;
import com.teamEWSN.gitdeun.notification.service.NotificationService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;
    private final NotificationService notificationService;

    /**
     * 모집 공고에 지원하기
     */
    @Transactional
    public ApplicationResponseDto createApplication(Long recruitmentId, ApplicationCreateRequestDto requestDto, Long userId) {
        // 지원자 정보 조회
        User applicant = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 모집 공고 정보 조회
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
            .orElseThrow(() -> {
                log.error("Recruitment not found with id: {}", recruitmentId);
                return new GlobalException(ErrorCode.RECRUITMENT_NOT_FOUND);
            });

        // 모집 상태 확인
        if (recruitment.getStatus() != RecruitmentStatus.RECRUITING) {
            log.warn("Application attempt to non-recruiting post: {}", recruitmentId);
            throw new GlobalException(ErrorCode.RECRUITMENT_NOT_RECRUITING);
        }

        // 모집 마감일 확인
        if (recruitment.getEndAt().isBefore(LocalDateTime.now())) {
            log.warn("Application attempt to expired recruitment: {}", recruitmentId);
            throw new GlobalException(ErrorCode.RECRUITMENT_EXPIRED);
        }

        // 본인 공고 지원 불가
        if (recruitment.getRecruiter().getId().equals(userId)) {
            log.warn("Self-application attempt by user: {}", userId);
            throw new GlobalException(ErrorCode.CANNOT_APPLY_OWN_RECRUITMENT);
        }

        // 중복 지원 확인
        if (applicationRepository.existsByRecruitmentAndApplicantAndActiveTrue(recruitment, applicant)) {
            log.warn("Duplicate application attempt by user: {} to recruitment: {}", userId, recruitmentId);
            throw new GlobalException(ErrorCode.ALREADY_APPLIED);
        }

        // 모집 인원 확인
        if (recruitment.getRecruitQuota() <= 0) {
            log.warn("Application to full recruitment: {}", recruitmentId);
            throw new GlobalException(ErrorCode.RECRUITMENT_FULL);
        }

        // 지원서 생성
        Application application = Application.builder()
            .recruitment(recruitment)
            .applicant(applicant)
            .appliedField(requestDto.getAppliedField())
            .message(requestDto.getMessage())
            .status(ApplicationStatus.PENDING)
            .active(true)
            .build();

        Application savedApplication = applicationRepository.save(application);


        // 모집자에게 알림 전송
        String notificationMessage = String.format(
            "'%s'님이 '%s' 공고에 지원했습니다.",
            applicant.getName(),
            recruitment.getTitle()
        );

        notificationService.createAndSendNotification(
            NotificationCreateDto.actionable(
                recruitment.getRecruiter(),
                NotificationType.APPLICATION_RECEIVED,
                notificationMessage,
                savedApplication.getId(),
                null
            )
        );
        
        return applicationMapper.toResponseDto(savedApplication);
    }

    /**
     * 내 지원 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<ApplicationListResponseDto> getMyApplications(Long userId, Pageable pageable) {
        User applicant = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Page<Application> applications = applicationRepository
            .findByApplicantAndActiveTrueOrderByCreatedAtDesc(applicant, pageable);

        return applications.map(applicationMapper::toListResponseDto);
    }

    /**
     * 특정 공고의 지원자 목록 조회 (모집자만 가능)
     */
    @Transactional(readOnly = true)
    public Page<ApplicationListResponseDto> getRecruitmentApplications(
        Long recruitmentId, Long userId, Pageable pageable) {

        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.RECRUITMENT_NOT_FOUND));

        // 모집자 본인 확인
        if (!recruitment.getRecruiter().getId().equals(userId)) {
            log.warn("Unauthorized access to applications by user: {} for recruitment: {}", userId, recruitmentId);
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Page<Application> applications = applicationRepository
            .findByRecruitmentAndActiveTrueOrderByCreatedAtDesc(recruitment, pageable);

        return applications.map(applicationMapper::toListResponseDto);
    }

    /**
     * 지원 상세 조회
     */
    @Transactional(readOnly = true)
    public ApplicationResponseDto getApplication(Long applicationId, Long userId) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new GlobalException(ErrorCode.APPLICATION_NOT_FOUND));

        // 지원자 본인 또는 모집자만 조회 가능
        boolean isApplicant = application.getApplicant().getId().equals(userId);
        boolean isRecruiter = application.getRecruitment().getRecruiter().getId().equals(userId);

        if (!isApplicant && !isRecruiter) {
            log.warn("Unauthorized access to application: {} by user: {}", applicationId, userId);
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return applicationMapper.toResponseDto(application);
    }

    /**
     * 지원 철회 (지원자만 가능)
     */
    @Transactional
    public void withdrawApplication(Long applicationId, Long userId) {
        Application application = applicationRepository.findByIdAndApplicant(applicationId,
                userRepository.findById(userId)
                    .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID)))
            .orElseThrow(() -> new GlobalException(ErrorCode.APPLICATION_NOT_FOUND));

        // 이미 철회된 지원인지 확인
        if (!application.isActive()) {
            throw new GlobalException(ErrorCode.APPLICATION_ALREADY_WITHDRAWN);
        }

        // 만약 '수락'된 상태의 지원을 철회하는 경우
        boolean wasAccepted = application.getStatus() == ApplicationStatus.ACCEPTED;
        if (wasAccepted) {
            Recruitment recruitment = application.getRecruitment();
            recruitment.increaseQuota();

            // 철회 알림
            String notificationMessage = String.format(
                "'%s'님이 '%s' 공고의 수락을 철회했습니다.",
                application.getApplicant().getName(),
                application.getRecruitment().getTitle()
            );

            notificationService.createAndSendNotification(
                NotificationCreateDto.simple(
                    application.getRecruitment().getRecruiter(),
                    NotificationType.APPLICATION_WITHDRAWN_AFTER_ACCEPTANCE,
                    notificationMessage
                )
            );
        }

        // 지원 철회 처리
        application.withdraw();

    }

    /**
     * 지원 수락 (모집자만 가능)
     */
    @Transactional
    public ApplicationResponseDto acceptApplication(Long applicationId, Long userId) {
        User recruiter = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Application application = applicationRepository.findByIdAndRecruiter(applicationId, recruiter)
            .orElseThrow(() -> new GlobalException(ErrorCode.APPLICATION_NOT_FOUND));

        // 활성 상태 확인
        if (!application.isActive()) {
            throw new GlobalException(ErrorCode.APPLICATION_NOT_ACTIVE);
        }

        // 이미 처리된 지원인지 확인
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new GlobalException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        // 수락 처리
        application.accept();

        // 모집 인원 감소
        Recruitment recruitment = application.getRecruitment();
        recruitment.decreaseQuota();

        // 지원자에게 알림 전송
        String notificationMessage = String.format(
            "'%s' 공고 지원이 수락되었습니다!",
            application.getRecruitment().getTitle()
        );

        notificationService.createAndSendNotification(
            NotificationCreateDto.simple(
                application.getApplicant(),
                NotificationType.APPLICATION_ACCEPTED,
                notificationMessage
            )
        );

        return applicationMapper.toResponseDto(application);
    }

    /**
     * 지원 거절 (모집자만 가능)
     */
    @Transactional
    public ApplicationResponseDto rejectApplication(
        Long applicationId, Long userId, ApplicationStatusUpdateDto updateDto) {

        User recruiter = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Application application = applicationRepository.findByIdAndRecruiter(applicationId, recruiter)
            .orElseThrow(() -> new GlobalException(ErrorCode.APPLICATION_NOT_FOUND));

        // 활성 상태 확인
        if (!application.isActive()) {
            throw new GlobalException(ErrorCode.APPLICATION_NOT_ACTIVE);
        }

        // 이미 처리된 지원인지 확인
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new GlobalException(ErrorCode.APPLICATION_ALREADY_PROCESSED);
        }

        // 거절 처리
        application.reject(updateDto.getRejectReason());

        // 지원자에게 알림 전송
        String notificationMessage = String.format(
            "'%s' 공고 지원이 거절되었습니다.",
            application.getRecruitment().getTitle()
        );

        if (updateDto.getRejectReason() != null && !updateDto.getRejectReason().isEmpty()) {
            notificationMessage += " 사유: " + updateDto.getRejectReason();
        }

        notificationService.createAndSendNotification(
            NotificationCreateDto.simple(
                application.getApplicant(),
                NotificationType.APPLICATION_REJECTED,
                notificationMessage
            )
        );

        return applicationMapper.toResponseDto(application);
    }
}